package main

import (
 "bytes"
 "context"
 "encoding/json"
 "net/http"
 "net/http/httptest"
 "os"
 "path/filepath"
 "strings"
 "sync"
 "testing"
 "time"
 "github.com/jackc/pgx/v5/pgxpool"
)

// Adapter for Core v30 symbols. The installer compiles against the real Core before
// deployment; these tests exercise the module against a disposable PostgreSQL database.
type App struct {db *pgxpool.Pool}
type testClaims struct {PlayerID int64;ServerID int}
type claimsKey struct{}
func claims(r *http.Request)testClaims {v,_:=r.Context().Value(claimsKey{}).(testClaims);return v}
func (a *App)v10Auth(h http.HandlerFunc)http.HandlerFunc{return h}
func v6Register(mux *http.ServeMux,path string,h http.HandlerFunc){mux.HandleFunc(path,h);mux.HandleFunc("/api/v1"+path,h)}
func method(w http.ResponseWriter,r *http.Request,m string)bool{if r.Method!=m{w.WriteHeader(405);return false};return true}
func writeJSON(w http.ResponseWriter,status int,value any){w.Header().Set("Content-Type","application/json");w.WriteHeader(status);_ = json.NewEncoder(w).Encode(value)}

func TestExpansion(t *testing.T) {
 ctx:=context.Background()
 config,err:=pgxpool.ParseConfig(os.Getenv("DATABASE_URL"));if err!=nil{t.Fatal(err)}
 if config.ConnConfig.Database!="frontier_expansion_test" {t.Fatal("Only the disposable frontier_expansion_test database is permitted")}
 db,err:=pgxpool.NewWithConfig(ctx,config);if err!=nil{t.Fatal(err)};defer db.Close()
 exec:=func(sql string,args ...any){t.Helper();if _,err:=db.Exec(ctx,sql,args...);err!=nil{t.Fatal(err)}}
 scalar:=func(sql string,args ...any)int64{t.Helper();var n int64;if err:=db.QueryRow(ctx,sql,args...).Scan(&n);err!=nil{t.Fatal(err)};return n}
 exec("DROP SCHEMA public CASCADE; CREATE SCHEMA public;")
 for _,name:=range []string{"validation/base.sql","001_expansion.sql"} {data,e:=os.ReadFile(filepath.Join(os.Getenv("FRONTIER_MODULE_DIR"),name));if e!=nil{t.Fatal(e)};exec(string(data))}
 exec(`INSERT INTO players VALUES(1,1,10000),(2,1,5000),(3,2,8000);
 INSERT INTO resources(player_id,food,wood,stone,gold) SELECT id,1000000,1000000,1000000,1000000 FROM players;
 INSERT INTO buildings(player_id,building_type,level) VALUES(1,'castle',30),(2,'castle',1),(3,'castle',30);
 INSERT INTO v12_world_cities(player_id,server_id,x,y) VALUES(1,1,250,250),(2,1,255,255),(3,2,250,250);`)
 app:=&App{db:db};mux:=http.NewServeMux();registerFrontierExpansionRoutes(mux,app)
 request:=func(player int64,server int,path,key,body string)*httptest.ResponseRecorder {
  m:="GET";if body!=""{m="POST"};r:=httptest.NewRequest(m,path,strings.NewReader(body));r.Header.Set("X-Idempotency-Key",key)
  r=r.WithContext(context.WithValue(r.Context(),claimsKey{},testClaims{player,server}));w:=httptest.NewRecorder();mux.ServeHTTP(w,r);return w
 }
 ok:=func(w *httptest.ResponseRecorder){t.Helper();if w.Code!=200{t.Fatalf("HTTP %d: %s",w.Code,w.Body.String())}}
 qty:=func(key string)int64{return scalar(`SELECT quantity FROM v5_player_inventory WHERE player_id=1 AND item_key=$1`,key)}
 t.Run("population_stable_unique_scoped_and_preserves_depletion",func(t *testing.T){
  exec(`SELECT frontier_seed(1)`)
  for table,count:=range map[string]int64{"frontier_npc_cities":1000,"v15_world_resource_nodes":2200,"v16_world_monsters":800,"frontier_landmarks":12} {if scalar("SELECT count(*) FROM "+table+" WHERE server_id=1")!=count{t.Fatal(table)}}
  if scalar(`SELECT count(DISTINCT power) FROM frontier_npc_cities`)<990{t.Fatal("powers lack variety")}
  if scalar(`SELECT count(*) FROM (SELECT x,y FROM frontier_npc_cities UNION ALL SELECT x,y FROM frontier_landmarks UNION ALL SELECT x,y FROM v15_world_resource_nodes UNION ALL SELECT x,y FROM v16_world_monsters UNION ALL SELECT x,y FROM v12_world_cities WHERE server_id=1)x`)!=(4012+2){t.Fatal("population size")}
  if scalar(`SELECT count(*) FROM (SELECT x,y FROM frontier_npc_cities UNION SELECT x,y FROM frontier_landmarks UNION SELECT x,y FROM v15_world_resource_nodes UNION SELECT x,y FROM v16_world_monsters UNION SELECT x,y FROM v12_world_cities WHERE server_id=1)x`)!=(4012+2){t.Fatal("overlapping objects")}
  var fingerprint string;err:=db.QueryRow(ctx,`SELECT md5(string_agg(row_to_json(c)::text,',' ORDER BY id)) FROM frontier_npc_cities c`).Scan(&fingerprint);if err!=nil{t.Fatal(err)}
  exec(`UPDATE v15_world_resource_nodes SET remaining_amount=0,status='depleted' WHERE id=(SELECT min(id) FROM v15_world_resource_nodes)`)
  exec(`SELECT frontier_seed(1)`)
  var after string;_ = db.QueryRow(ctx,`SELECT md5(string_agg(row_to_json(c)::text,',' ORDER BY id)) FROM frontier_npc_cities c`).Scan(&after)
  if after!=fingerprint{t.Fatal("seed changed cities")};if scalar(`SELECT count(*) FROM v15_world_resource_nodes WHERE status='depleted'`)!=1{t.Fatal("depleted node reset")}
  if scalar(`SELECT count(*) FROM frontier_npc_cities WHERE server_id=2`)!=0{t.Fatal("seed leaked across kingdoms")}
  ok(request(1,1,"/api/v1/expansion/v1/world","",""))
  other:=request(3,2,"/expansion/v1/world","","");ok(other)
  if !bytes.Contains(other.Body.Bytes(),[]byte(`"npc_count":0`)){t.Fatal(other.Body.String())}
 })
 t.Run("starter_once_and_atomic_replay",func(t *testing.T){
  first:=request(1,1,"/expansion/v1/starter-claim","starter-test-001","{}");ok(first)
  second:=request(1,1,"/expansion/v1/starter-claim","starter-test-001","{}");ok(second)
  if !bytes.Equal(first.Body.Bytes(),second.Body.Bytes()){t.Fatal("replay differs")}
  if qty("frontier_speed_60")!=20{t.Fatal("starter duplicated")}
  if request(1,1,"/expansion/v1/starter-claim","starter-test-002","{}").Code!=409{t.Fatal("second starter allowed")}
 })
 t.Run("speedup_replay_and_body_binding",func(t *testing.T){
  exec(`INSERT INTO training_queue(id,player_id,troop_type,amount,finishes_at) VALUES(1001,1,'infantry',1000,now()+interval '30 minutes')`)
  body:=`{"kind":"train","job_id":1001,"item_key":"frontier_speed_60","count":2}`
  first:=request(1,1,"/expansion/v1/speedup","speedup-test-001",body);ok(first)
  second:=request(1,1,"/expansion/v1/speedup","speedup-test-001",body);ok(second)
  if qty("frontier_speed_60")!=18 || !bytes.Equal(first.Body.Bytes(),second.Body.Bytes()){t.Fatal("replay spent twice")}
  if scalar(`SELECT round(extract(epoch from finishes_at-started_at)) FROM training_queue WHERE id=1001`)!=1680{t.Fatal("wrong finish")}
  if request(1,1,"/expansion/v1/speedup","speedup-test-001",strings.Replace(body,`"count":2`,`"count":3`,1)).Code!=409{t.Fatal("body mismatch allowed")}
 })
 t.Run("concurrent_duplicate_spends_once",func(t *testing.T){
  var wg sync.WaitGroup;codes:=make(chan int,8)
  for i:=0;i<8;i++{wg.Add(1);go func(){defer wg.Done();codes<-request(1,1,"/expansion/v1/speedup","concurrent-test-001",`{"kind":"train","job_id":1001,"item_key":"frontier_speed_60","count":1}`).Code}()};wg.Wait();close(codes)
  for code:=range codes{if code!=200{t.Fatalf("concurrent status %d",code)}}
  if qty("frontier_speed_60")!=17{t.Fatal("concurrent double spend")}
 })
 t.Run("reject_wrong_owner_kind_count_complete_and_excess",func(t *testing.T){
  exec(`INSERT INTO build_queue(id,player_id,building_type,target_level,finishes_at) VALUES(2001,1,'farm',2,now()+interval '10 seconds'),(2002,1,'farm',3,now()-interval '10 seconds')`)
  cases:=[]struct{p int64;s int;b string;status int}{
   {2,1,`{"kind":"train","job_id":1001,"item_key":"frontier_speed_60","count":1}`,404},
   {1,2,`{"kind":"train","job_id":1001,"item_key":"frontier_speed_60","count":1}`,403},
   {1,1,`{"kind":"build","job_id":2001,"item_key":"frontier_train_300","count":1}`,400},
   {1,1,`{"kind":"train","job_id":1001,"item_key":"frontier_speed_60","count":0}`,400},
   {1,1,`{"kind":"train","job_id":1001,"item_key":"frontier_speed_60","count":999}`,409},
   {1,1,`{"kind":"build","job_id":2001,"item_key":"frontier_speed_60","count":2}`,409},
   {1,1,`{"kind":"build","job_id":2002,"item_key":"frontier_speed_60","count":1}`,409},
  }
  for _,c:=range cases{w:=request(c.p,c.s,"/expansion/v1/speedup","reject-test-key",c.b);if w.Code!=c.status{t.Fatalf("expected %d got %d: %s",c.status,w.Code,w.Body.String())}}
  if qty("frontier_speed_60")!=17{t.Fatal("rejected request spent items")}
  ok(request(1,1,"/expansion/v1/speedup","complete-test-key",`{"kind":"build","job_id":2001,"item_key":"frontier_speed_60","count":1}`))
  if scalar(`SELECT count(*) FROM build_queue WHERE id=2001 AND finishes_at<=now()`)!=1{t.Fatal("speedup did not complete job")}
  if scalar(`SELECT count(*) FROM build_queue WHERE id=2001 AND claimed`)!=0{t.Fatal("speedup claimed without player action")}
 })
 t.Run("landmark_requirements_costs_bonuses_and_one_time_power",func(t *testing.T){
  if request(2,1,"/expansion/v1/claim-landmark","castle-low-test",`{"landmark_id":1}`).Code!=409{t.Fatal("castle gate")}
  before:=scalar(`SELECT food FROM resources WHERE player_id=1`)
  ok(request(1,1,"/expansion/v1/claim-landmark","landmark-test-001",`{"landmark_id":1}`))
  ok(request(1,1,"/expansion/v1/claim-landmark","landmark-test-002",`{"landmark_id":2}`))
  ok(request(1,1,"/expansion/v1/claim-landmark","landmark-test-003",`{"landmark_id":3}`))
  if scalar(`SELECT food FROM resources WHERE player_id=1`)!=before-15000{t.Fatal("landmark costs")}
  if scalar(`SELECT power FROM players WHERE id=1`)!=12500{t.Fatal("power bonus")}
  if request(1,1,"/expansion/v1/claim-landmark","landmark-duplicate",`{"landmark_id":3}`).Code!=409{t.Fatal("repeat landmark")}
  if scalar(`SELECT power FROM players WHERE id=1`)!=12500{t.Fatal("power duplicated")}
  exec(`INSERT INTO training_queue(id,player_id,troop_type,amount,finishes_at) VALUES(1002,1,'archers',1000,now()+interval '1050 seconds')`)
  if scalar(`SELECT round(extract(epoch from finishes_at-started_at)) FROM training_queue WHERE id=1002`)!=1000{t.Fatal("training bonus not applied")}
  exec(`INSERT INTO v15_gather_marches(server_id,player_id,node_id,travel_seconds,gather_seconds,arrive_at) SELECT 1,1,min(id),5,105,now()+interval '5 seconds' FROM v15_world_resource_nodes`)
  if scalar(`SELECT gather_seconds FROM v15_gather_marches LIMIT 1`)!=100{t.Fatal("gather bonus not applied")}
  if scalar(`SELECT frontier_bonus(3,'training')`)!=0{t.Fatal("bonus leaked across players")}
  state:=request(1,1,"/expansion/v1/state","","");ok(state)
  var doc map[string]any;if err:=json.Unmarshal(state.Body.Bytes(),&doc);err!=nil{t.Fatal(err)}
  if doc["training_percent"].(float64)!=5 || len(doc["jobs"].([]any))!=4{t.Fatal(doc)}
 })
 t.Run("city_collision_and_read_only_methods",func(t *testing.T){
  _,err:=db.Exec(ctx,`UPDATE v12_world_cities SET x=(SELECT x FROM frontier_npc_cities WHERE id=1 AND server_id=1),y=(SELECT y FROM frontier_npc_cities WHERE id=1 AND server_id=1) WHERE player_id=1`)
  if err==nil{t.Fatal("NPC city overlap accepted")}
  if request(1,1,"/expansion/v1/state","method-test-key","{}").Code!=405{t.Fatal("read endpoint accepted POST")}
 })
 t.Run("missing_idempotency_key_rejected",func(t *testing.T){if request(1,1,"/expansion/v1/starter-claim","","{}").Code!=400{t.Fatal("missing key accepted")}})
 _=time.Second
}
