package main

import (
 "context"
 "crypto/sha256"
 "encoding/hex"
 "encoding/json"
 "errors"
 "io"
 "net/http"
 "time"

 "github.com/jackc/pgx/v5"
)

func registerFrontierExpansionRoutes(mux *http.ServeMux, a *App) {
 v6Register(mux,"/expansion/v1/world",a.v10Auth(a.frontierWorld))
 v6Register(mux,"/expansion/v1/state",a.v10Auth(a.frontierState))
 for _, path := range []string{"speedup","claim-landmark","starter-claim"} {
  v6Register(mux,"/expansion/v1/"+path,a.v10Auth(a.frontierCommand))
 }
}

func (a *App) frontierWorld(w http.ResponseWriter,r *http.Request) {
 if !method(w,r,http.MethodGet) { return }
 var result []byte
 err:=a.db.QueryRow(r.Context(),`SELECT jsonb_build_object(
  'version',1,'npc_count',(SELECT count(*) FROM frontier_npc_cities WHERE server_id=$1),
  'cities',COALESCE((SELECT jsonb_agg(to_jsonb(c) ORDER BY c.id) FROM frontier_npc_cities c WHERE server_id=$1),'[]'::jsonb),
  'landmarks',COALESCE((SELECT jsonb_agg(to_jsonb(l) ORDER BY l.id) FROM frontier_landmarks l WHERE server_id=$1),'[]'::jsonb))`,claims(r).ServerID).Scan(&result)
 if err!=nil { writeJSON(w,503,map[string]string{"error":"world expansion unavailable"});return }
 writeJSON(w,200,json.RawMessage(result))
}

func (a *App) frontierState(w http.ResponseWriter,r *http.Request) {
 if !method(w,r,http.MethodGet) { return }
 pc:=claims(r)
 var result []byte
 err:=a.db.QueryRow(r.Context(),`SELECT jsonb_build_object(
  'version',1,'training_percent',frontier_bonus($1,'training'),
  'gathering_percent',frontier_bonus($1,'gathering'),'power_points',frontier_bonus($1,'power'),
  'starter_claimed',EXISTS(SELECT 1 FROM frontier_starter_claims WHERE player_id=$1),
  'claimed_landmarks',COALESCE((SELECT jsonb_agg(landmark_id) FROM frontier_landmark_claims WHERE player_id=$1 AND server_id=$2),'[]'::jsonb),
  'jobs',COALESCE((SELECT jsonb_agg(j ORDER BY j.ends) FROM (
   SELECT 'build' AS kind,id,building_type AS label,finishes_at AS ends FROM build_queue WHERE player_id=$1 AND NOT claimed
   UNION ALL SELECT 'train',id,troop_type,finishes_at FROM training_queue WHERE player_id=$1 AND NOT claimed
   UNION ALL SELECT 'research',id,research_key,finishes_at FROM research_queue WHERE player_id=$1 AND NOT claimed
  )j),'[]'::jsonb))`,pc.PlayerID,pc.ServerID).Scan(&result)
 if err!=nil {writeJSON(w,503,map[string]string{"error":"expansion state unavailable"});return}
 writeJSON(w,200,json.RawMessage(result))
}

type frontierRequest struct {
 Kind string `json:"kind"`
 JobID int64 `json:"job_id"`
 ItemKey string `json:"item_key"`
 Count int64 `json:"count"`
 LandmarkID int `json:"landmark_id"`
}
type frontierRejection struct { code int; message string }
func (e frontierRejection) Error() string {return e.message}
func frontierReject(code int,message string) error {return frontierRejection{code,message}}

// One player lock serializes different command keys. The persisted response shares the
// transaction with its effect, so lost HTTP responses never spend an item twice.
func (a *App) frontierCommand(w http.ResponseWriter,r *http.Request) {
 if !method(w,r,http.MethodPost) {return}
 pc:=claims(r); key:=r.Header.Get("X-Idempotency-Key")
 if len(key)<8 || len(key)>128 {writeJSON(w,400,map[string]string{"error":"idempotency key required"});return}
 body,err:=io.ReadAll(io.LimitReader(r.Body,4097))
 var req frontierRequest
 if err!=nil || len(body)>4096 || json.Unmarshal(body,&req)!=nil {writeJSON(w,400,map[string]string{"error":"invalid command"});return}
 digest:=sha256.Sum256(append([]byte(r.URL.Path+"\n"),body...)); hash:=hex.EncodeToString(digest[:])
 tx,err:=a.db.Begin(r.Context());if err!=nil {writeJSON(w,503,map[string]string{"error":"database unavailable"});return}
 defer tx.Rollback(r.Context())
 var owner int64
 err=tx.QueryRow(r.Context(),`SELECT id FROM players WHERE id=$1 AND server_id=$2 FOR UPDATE`,pc.PlayerID,pc.ServerID).Scan(&owner)
 if err!=nil {writeJSON(w,403,map[string]string{"error":"wrong kingdom"});return}
 var oldHash string;var response []byte
 err=tx.QueryRow(r.Context(),`SELECT request_hash,response FROM frontier_commands WHERE player_id=$1 AND command_key=$2`,pc.PlayerID,key).Scan(&oldHash,&response)
 if err==nil {
  if oldHash!=hash {writeJSON(w,409,map[string]string{"error":"command key belongs to another request"});return}
  writeJSON(w,200,json.RawMessage(response));return
 }
 if !errors.Is(err,pgx.ErrNoRows) {writeJSON(w,503,map[string]string{"error":"command lookup unavailable"});return}
 var value any
 switch r.URL.Path {
 case "/expansion/v1/speedup", "/api/v1/expansion/v1/speedup": value,err=frontierSpeedup(r.Context(),tx,pc.PlayerID,req)
 case "/expansion/v1/claim-landmark", "/api/v1/expansion/v1/claim-landmark": value,err=frontierClaim(r.Context(),tx,pc.PlayerID,pc.ServerID,req.LandmarkID)
 case "/expansion/v1/starter-claim", "/api/v1/expansion/v1/starter-claim": value,err=frontierStarter(r.Context(),tx,pc.PlayerID)
 default: err=frontierReject(404,"unknown expansion command")
 }
 if err!=nil {
  var rejection frontierRejection
  if errors.As(err,&rejection) {writeJSON(w,rejection.code,map[string]string{"error":rejection.message})
  } else {writeJSON(w,503,map[string]string{"error":"command could not be completed; retry safely"})};return
 }
 response,err=json.Marshal(value)
 if err==nil {_,err=tx.Exec(r.Context(),`INSERT INTO frontier_commands(player_id,command_key,request_hash,response) VALUES($1,$2,$3,$4)`,pc.PlayerID,key,hash,response)}
 if err==nil {err=tx.Commit(r.Context())}
 if err!=nil {writeJSON(w,503,map[string]string{"error":"command confirmation unavailable; retry safely"});return}
 writeJSON(w,200,json.RawMessage(response))
}

func frontierSpeedup(ctx context.Context,tx pgx.Tx,player int64,req frontierRequest)(any,error) {
 table:=map[string]string{"build":"build_queue","train":"training_queue","research":"research_queue"}[req.Kind]
 if table=="" || req.JobID<=0 || req.Count<1 || req.Count>10000 {return nil,frontierReject(400,"invalid speedup target or count")}
 var finish,now time.Time
 err:=tx.QueryRow(ctx,`SELECT finishes_at,clock_timestamp() FROM `+table+` WHERE id=$1 AND player_id=$2 AND NOT claimed FOR UPDATE`,req.JobID,player).Scan(&finish,&now)
 if errors.Is(err,pgx.ErrNoRows) {return nil,frontierReject(404,"job not found")};if err!=nil{return nil,err}
 if !finish.After(now) {return nil,frontierReject(409,"job already finished; collect it without a speedup")}
 var kind string;var effect,qty int64
 err=tx.QueryRow(ctx,`SELECT d.item_type,d.effect_value,i.quantity FROM v5_player_inventory i
 JOIN v5_item_definitions d USING(item_key) WHERE i.player_id=$1 AND i.item_key=$2 FOR UPDATE OF i`,player,req.ItemKey).Scan(&kind,&effect,&qty)
 if errors.Is(err,pgx.ErrNoRows) {return nil,frontierReject(404,"speedup item not found")};if err!=nil{return nil,err}
 if (kind!="speedup" && kind!="speedup_"+req.Kind) || effect<1 || effect>86400 {return nil,frontierReject(400,"incompatible speedup")}
 if qty<req.Count {return nil,frontierReject(409,"not enough speedups")}
 remaining:=finish.Sub(now)
 maxCount:=int64((remaining+time.Duration(effect)*time.Second-1)/(time.Duration(effect)*time.Second))
 if req.Count>maxCount {return nil,frontierReject(409,"fewer speedups are enough; refresh the remaining time")}
 next:=finish.Add(-time.Duration(req.Count*effect)*time.Second);if next.Before(now){next=now}
 if _,err=tx.Exec(ctx,`UPDATE `+table+` SET finishes_at=$2 WHERE id=$1`,req.JobID,next);err!=nil{return nil,err}
 if _,err=tx.Exec(ctx,`UPDATE v5_player_inventory SET quantity=quantity-$3,updated_at=NOW() WHERE player_id=$1 AND item_key=$2`,player,req.ItemKey,req.Count);err!=nil{return nil,err}
 return map[string]any{"job_id":req.JobID,"kind":req.Kind,"finishes_at":next,"used":req.Count},nil
}

func frontierStarter(ctx context.Context,tx pgx.Tx,player int64)(any,error) {
 result,err:=tx.Exec(ctx,`INSERT INTO frontier_starter_claims(player_id) VALUES($1) ON CONFLICT DO NOTHING`,player)
 if err!=nil{return nil,err};if result.RowsAffected()==0{return nil,frontierReject(409,"starter pack already claimed")}
 _,err=tx.Exec(ctx,`INSERT INTO v5_player_inventory(player_id,item_key,quantity) VALUES
 ($1,'frontier_speed_60',20),($1,'frontier_speed_300',10),($1,'frontier_speed_3600',2),
 ($1,'frontier_train_300',5),($1,'frontier_build_300',5),($1,'frontier_research_300',5)
 ON CONFLICT(player_id,item_key) DO UPDATE SET quantity=v5_player_inventory.quantity+EXCLUDED.quantity,updated_at=NOW()`,player)
 return map[string]any{"claimed":true},err
}

func frontierClaim(ctx context.Context,tx pgx.Tx,player int64,server,id int)(any,error) {
 var kind string;var bonus,required,castle int;var cost int64;var owned bool
 err:=tx.QueryRow(ctx,`SELECT kind,bonus,castle_level,cost FROM frontier_landmarks WHERE server_id=$1 AND id=$2`,server,id).Scan(&kind,&bonus,&required,&cost)
 if errors.Is(err,pgx.ErrNoRows){return nil,frontierReject(404,"landmark not found")};if err!=nil{return nil,err}
 err=tx.QueryRow(ctx,`SELECT EXISTS(SELECT 1 FROM frontier_landmark_claims WHERE player_id=$1 AND landmark_id=$2)`,player,id).Scan(&owned)
 if err!=nil{return nil,err};if owned{return nil,frontierReject(409,"landmark already activated")}
 err=tx.QueryRow(ctx,`SELECT level FROM buildings WHERE player_id=$1 AND building_type='castle'`,player).Scan(&castle)
 if err!=nil{return nil,err};if castle<required{return nil,frontierReject(409,"castle level too low")}
 result,err:=tx.Exec(ctx,`UPDATE resources SET food=food-$2,wood=wood-$2,updated_at=NOW() WHERE player_id=$1 AND food>=$2 AND wood>=$2`,player,cost)
 if err!=nil{return nil,err};if result.RowsAffected()!=1{return nil,frontierReject(409,"not enough food or wood")}
 _,err=tx.Exec(ctx,`INSERT INTO frontier_landmark_claims(server_id,landmark_id,player_id) VALUES($1,$2,$3)`,server,id,player)
 if err!=nil{return nil,err}
 if kind=="power" {_,err=tx.Exec(ctx,`UPDATE players SET power=power+$2 WHERE id=$1`,player,bonus)}
 return map[string]any{"landmark_id":id,"activated":true,"kind":kind,"bonus":bonus},err
}
