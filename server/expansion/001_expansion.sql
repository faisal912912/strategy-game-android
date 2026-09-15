BEGIN;

CREATE TABLE IF NOT EXISTS frontier_npc_cities (
 server_id integer NOT NULL REFERENCES game_servers(id),
 id integer NOT NULL, name text NOT NULL,
 x integer NOT NULL CHECK(x BETWEEN 0 AND 499), y integer NOT NULL CHECK(y BETWEEN 0 AND 499),
 level integer NOT NULL CHECK(level BETWEEN 1 AND 30), power bigint NOT NULL CHECK(power > 0),
 PRIMARY KEY(server_id,id), UNIQUE(server_id,x,y)
);
CREATE TABLE IF NOT EXISTS frontier_landmarks (
 server_id integer NOT NULL REFERENCES game_servers(id), id integer NOT NULL,
 name text NOT NULL, kind text NOT NULL CHECK(kind IN ('training','gathering','power')),
 x integer NOT NULL, y integer NOT NULL, bonus integer NOT NULL,
 castle_level integer NOT NULL, cost bigint NOT NULL,
 PRIMARY KEY(server_id,id), UNIQUE(server_id,x,y)
);
CREATE TABLE IF NOT EXISTS frontier_landmark_claims (
 server_id integer NOT NULL, landmark_id integer NOT NULL,
 player_id bigint NOT NULL REFERENCES players(id) ON DELETE CASCADE,
 claimed_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(player_id,landmark_id),
 FOREIGN KEY(server_id,landmark_id) REFERENCES frontier_landmarks(server_id,id)
);
CREATE TABLE IF NOT EXISTS frontier_seed_objects (
 server_id integer NOT NULL, kind text NOT NULL, ordinal integer NOT NULL,
 object_id bigint NOT NULL, PRIMARY KEY(server_id,kind,ordinal)
);
CREATE TABLE IF NOT EXISTS frontier_starter_claims (
 player_id bigint PRIMARY KEY REFERENCES players(id) ON DELETE CASCADE,
 claimed_at timestamptz NOT NULL DEFAULT now()
);
-- Response and economic mutation commit together, including crash/retry protection.
CREATE TABLE IF NOT EXISTS frontier_commands (
 player_id bigint NOT NULL REFERENCES players(id) ON DELETE CASCADE,
 command_key text NOT NULL, request_hash text NOT NULL, response jsonb NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(player_id,command_key)
);
INSERT INTO v5_item_definitions(item_key,name,item_type,effect_value) VALUES
 ('frontier_speed_60','تسريع عام • دقيقة','speedup',60),
 ('frontier_speed_300','تسريع عام • 5 دقائق','speedup',300),
 ('frontier_speed_3600','تسريع عام • ساعة','speedup',3600),
 ('frontier_train_300','تسريع تدريب • 5 دقائق','speedup_train',300),
 ('frontier_build_300','تسريع بناء • 5 دقائق','speedup_build',300),
 ('frontier_research_300','تسريع بحث • 5 دقائق','speedup_research',300)
ON CONFLICT(item_key) DO NOTHING;

CREATE OR REPLACE FUNCTION frontier_bonus(p bigint, k text) RETURNS integer
LANGUAGE sql STABLE AS $$
 SELECT COALESCE(sum(l.bonus),0)::integer FROM frontier_landmark_claims c
 JOIN frontier_landmarks l ON l.server_id=c.server_id AND l.id=c.landmark_id
 WHERE c.player_id=p AND l.kind=k
$$;
-- Bonuses apply at the beginning of new jobs; existing jobs retain their promised time.
CREATE OR REPLACE FUNCTION frontier_training_bonus() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 NEW.finishes_at := NEW.started_at + (NEW.finishes_at-NEW.started_at) /
   (1.0+frontier_bonus(NEW.player_id,'training')/100.0);
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS frontier_training_bonus ON training_queue;
CREATE TRIGGER frontier_training_bonus BEFORE INSERT ON training_queue
 FOR EACH ROW EXECUTE FUNCTION frontier_training_bonus();
CREATE OR REPLACE FUNCTION frontier_gather_bonus() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 NEW.gather_seconds := greatest(1,ceil(NEW.gather_seconds /
   (1.0+frontier_bonus(NEW.player_id,'gathering')/100.0))::integer);
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS frontier_gather_bonus ON v15_gather_marches;
CREATE TRIGGER frontier_gather_bonus BEFORE INSERT ON v15_gather_marches
 FOR EACH ROW EXECUTE FUNCTION frontier_gather_bonus();

-- A fixed permutation visits every tile once. Reruns preserve positions, IDs and depletion.
CREATE OR REPLACE FUNCTION frontier_seed(s integer) RETURNS void LANGUAGE plpgsql AS $$
DECLARE n integer := 0; i integer; px integer; py integer; tile integer; v integer;
 object_kind text; obj bigint; lv integer; resource text; monster text;
BEGIN
 PERFORM pg_advisory_xact_lock(601906,s);
 FOR i IN 1..4012 LOOP
  object_kind := CASE WHEN i<=1000 THEN 'city' WHEN i<=3200 THEN 'resource'
               WHEN i<=4000 THEN 'monster' ELSE 'landmark' END;
  IF (object_kind='city' AND EXISTS(SELECT 1 FROM frontier_npc_cities WHERE server_id=s AND id=i))
   OR (object_kind='landmark' AND EXISTS(SELECT 1 FROM frontier_landmarks WHERE server_id=s AND id=i-4000))
   OR EXISTS(SELECT 1 FROM frontier_seed_objects WHERE server_id=s AND ordinal=i AND frontier_seed_objects.kind=object_kind)
  THEN CONTINUE; END IF;
  LOOP
   IF n>=250000 THEN RAISE EXCEPTION 'No free world tile'; END IF;
   tile := ((n::bigint*104729+s::bigint*17317)%250000)::integer; n:=n+1;
   px:=tile%500; py:=tile/500;
   EXIT WHEN NOT EXISTS(SELECT 1 FROM v12_world_cities WHERE server_id=s AND x=px AND y=py)
    AND NOT EXISTS(SELECT 1 FROM v15_world_resource_nodes WHERE server_id=s AND x=px AND y=py)
    AND NOT EXISTS(SELECT 1 FROM v16_world_monsters WHERE server_id=s AND x=px AND y=py)
    AND NOT EXISTS(SELECT 1 FROM frontier_npc_cities WHERE server_id=s AND x=px AND y=py)
    AND NOT EXISTS(SELECT 1 FROM frontier_landmarks WHERE server_id=s AND x=px AND y=py);
  END LOOP;
  v:=('x'||substr(md5(s::text||':'||i::text),1,7))::bit(28)::integer;
  IF object_kind='city' THEN
   INSERT INTO frontier_npc_cities VALUES(s,i,
    (ARRAY['حصن الصقور','واحة الياقوت','قلعة الشمال','مدينة النخيل','حصن الرماد','مرفأ الشمس'])[1+v%6]||' '||i,
    px,py,3+v%28,5000+(v::bigint*7919)%89995001);
  ELSIF object_kind='resource' THEN
   lv:=1+v%10; resource:=(ARRAY['food','wood','stone','gold'])[1+(i%4)];
   INSERT INTO v15_world_resource_nodes(server_id,node_type,level,x,y,max_amount,remaining_amount)
    VALUES(s,resource,lv,px,py,lv*20000,lv*20000) RETURNING id INTO obj;
   INSERT INTO frontier_seed_objects VALUES(s,object_kind,i,obj);
  ELSIF object_kind='monster' THEN
   lv:=1+v%30; monster:=CASE WHEN i%20=0 THEN 'boss' WHEN i%5=0 THEN 'elite' ELSE 'normal' END;
   INSERT INTO v16_world_monsters(server_id,monster_type,name,level,x,y,max_hp,current_hp,power,
    reward_food,reward_wood,reward_stone,reward_gold)
   VALUES(s,monster,CASE monster WHEN 'boss' THEN 'ملك الوحوش' WHEN 'elite' THEN 'حارس الأطلال' ELSE 'ذئب البراري' END,
    lv,px,py,lv*800,lv*800,lv*250,lv*500,lv*500,lv*200,lv*100) RETURNING id INTO obj;
   INSERT INTO frontier_seed_objects VALUES(s,object_kind,i,obj);
  ELSE
   lv:=1+(i-4001)/3; object_kind:=(ARRAY['training','gathering','power'])[1+(i-4001)%3];
   INSERT INTO frontier_landmarks VALUES(s,i-4000,
    CASE object_kind WHEN 'training' THEN 'معسكر القادة' WHEN 'gathering' THEN 'منارة الحصاد' ELSE 'حصن الهيبة' END||' '||lv,
    object_kind,px,py,CASE object_kind WHEN 'power' THEN lv*2500 ELSE 5 END,lv*3,lv*5000);
  END IF;
 END LOOP;
END $$;
-- Keep subsequent city placements/teleports out of the new NPCs and landmarks.
CREATE OR REPLACE FUNCTION frontier_city_space() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS(SELECT 1 FROM frontier_npc_cities WHERE server_id=NEW.server_id AND x=NEW.x AND y=NEW.y)
  OR EXISTS(SELECT 1 FROM frontier_landmarks WHERE server_id=NEW.server_id AND x=NEW.x AND y=NEW.y)
 THEN RAISE unique_violation USING MESSAGE='world tile occupied'; END IF;
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS frontier_city_space ON v12_world_cities;
CREATE TRIGGER frontier_city_space BEFORE INSERT OR UPDATE OF x,y ON v12_world_cities
 FOR EACH ROW EXECUTE FUNCTION frontier_city_space();
COMMIT;
