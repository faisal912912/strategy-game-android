CREATE TABLE game_servers(id integer PRIMARY KEY);
INSERT INTO game_servers VALUES(1),(2);
CREATE TABLE players(id bigint PRIMARY KEY,server_id integer NOT NULL REFERENCES game_servers(id),power bigint NOT NULL DEFAULT 0);
CREATE TABLE resources(player_id bigint PRIMARY KEY REFERENCES players(id),food bigint DEFAULT 0,wood bigint DEFAULT 0,stone bigint DEFAULT 0,gold bigint DEFAULT 0,updated_at timestamptz DEFAULT now());
CREATE TABLE IF NOT EXISTS buildings (
  id BIGSERIAL PRIMARY KEY,
  player_id BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
  building_type VARCHAR(32) NOT NULL,
  level INT NOT NULL DEFAULT 1 CHECK (level >= 1),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(player_id, building_type)
);
CREATE TABLE IF NOT EXISTS build_queue (
  id BIGSERIAL PRIMARY KEY,
  player_id BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
  building_type VARCHAR(32) NOT NULL,
  target_level INT NOT NULL,
  started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  finishes_at TIMESTAMPTZ NOT NULL,
  claimed BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE IF NOT EXISTS training_queue (
  id BIGSERIAL PRIMARY KEY,
  player_id BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
  troop_type VARCHAR(16) NOT NULL,
  troop_tier INT NOT NULL DEFAULT 1,
  amount BIGINT NOT NULL CHECK (amount > 0),
  started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  finishes_at TIMESTAMPTZ NOT NULL,
  claimed BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE IF NOT EXISTS research_definitions (
  research_key VARCHAR(64) PRIMARY KEY,
  tree VARCHAR(16) NOT NULL,
  name VARCHAR(64) NOT NULL,
  max_level INT NOT NULL DEFAULT 10,
  base_seconds INT NOT NULL DEFAULT 60
);
CREATE TABLE IF NOT EXISTS research_queue (
  id BIGSERIAL PRIMARY KEY,
  player_id BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
  research_key VARCHAR(64) NOT NULL REFERENCES research_definitions(research_key),
  target_level INT NOT NULL,
  started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  finishes_at TIMESTAMPTZ NOT NULL,
  claimed BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE IF NOT EXISTS v5_item_definitions (
    item_key VARCHAR(64) PRIMARY KEY,
    name VARCHAR(96) NOT NULL,
    item_type VARCHAR(32) NOT NULL,
    effect_value BIGINT NOT NULL DEFAULT 0,
    stackable BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE IF NOT EXISTS v5_player_inventory (
    player_id BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    item_key VARCHAR(64) NOT NULL REFERENCES v5_item_definitions(item_key),
    quantity BIGINT NOT NULL DEFAULT 0 CHECK(quantity >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY(player_id,item_key)
);
CREATE TABLE IF NOT EXISTS v12_world_cities (
    server_id INT NOT NULL REFERENCES game_servers(id) ON DELETE CASCADE,
    player_id BIGINT PRIMARY KEY REFERENCES players(id) ON DELETE CASCADE,
    x INT NOT NULL CHECK(x >= 0),
    y INT NOT NULL CHECK(y >= 0),
    last_teleport_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(server_id, x, y)
);
CREATE TABLE IF NOT EXISTS v15_world_resource_nodes (
    id BIGSERIAL PRIMARY KEY,
    server_id INT NOT NULL REFERENCES game_servers(id) ON DELETE CASCADE,
    node_type VARCHAR(20) NOT NULL
        CHECK(node_type IN ('food','wood','stone','gold')),
    level INT NOT NULL DEFAULT 1 CHECK(level BETWEEN 1 AND 10),
    x INT NOT NULL CHECK(x >= 0),
    y INT NOT NULL CHECK(y >= 0),
    max_amount BIGINT NOT NULL CHECK(max_amount > 0),
    remaining_amount BIGINT NOT NULL CHECK(remaining_amount >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'active'
        CHECK(status IN ('active','depleted')),
    respawn_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(server_id, x, y)
);
CREATE TABLE IF NOT EXISTS v15_gather_marches (
    id BIGSERIAL PRIMARY KEY,
    server_id INT NOT NULL REFERENCES game_servers(id) ON DELETE CASCADE,
    player_id BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    node_id BIGINT NOT NULL REFERENCES v15_world_resource_nodes(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL DEFAULT 'outbound'
        CHECK(status IN ('outbound','gathering','returning','completed')),
    infantry BIGINT NOT NULL DEFAULT 0 CHECK(infantry >= 0),
    cavalry BIGINT NOT NULL DEFAULT 0 CHECK(cavalry >= 0),
    archers BIGINT NOT NULL DEFAULT 0 CHECK(archers >= 0),
    travel_seconds INT NOT NULL CHECK(travel_seconds >= 1),
    gather_seconds INT NOT NULL CHECK(gather_seconds >= 1),
    territory_bonus_percent INT NOT NULL DEFAULT 0
        CHECK(territory_bonus_percent BETWEEN 0 AND 200),
    load_base BIGINT NOT NULL DEFAULT 0 CHECK(load_base >= 0),
    load_bonus BIGINT NOT NULL DEFAULT 0 CHECK(load_bonus >= 0),
    load_total BIGINT NOT NULL DEFAULT 0 CHECK(load_total >= 0),
    depart_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    arrive_at TIMESTAMPTZ NOT NULL,
    gather_started_at TIMESTAMPTZ,
    gather_complete_at TIMESTAMPTZ,
    return_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    recalled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE IF NOT EXISTS v16_world_monsters (
    id BIGSERIAL PRIMARY KEY,
    server_id INT NOT NULL REFERENCES game_servers(id) ON DELETE CASCADE,
    monster_type VARCHAR(20) NOT NULL
        CHECK(monster_type IN ('normal','elite','boss')),
    name VARCHAR(80) NOT NULL,
    level INT NOT NULL DEFAULT 1 CHECK(level BETWEEN 1 AND 100),
    x INT NOT NULL CHECK(x >= 0),
    y INT NOT NULL CHECK(y >= 0),
    max_hp BIGINT NOT NULL CHECK(max_hp > 0),
    current_hp BIGINT NOT NULL CHECK(current_hp >= 0),
    power BIGINT NOT NULL CHECK(power > 0),
    reward_food BIGINT NOT NULL DEFAULT 0 CHECK(reward_food >= 0),
    reward_wood BIGINT NOT NULL DEFAULT 0 CHECK(reward_wood >= 0),
    reward_stone BIGINT NOT NULL DEFAULT 0 CHECK(reward_stone >= 0),
    reward_gold BIGINT NOT NULL DEFAULT 0 CHECK(reward_gold >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'active'
        CHECK(status IN ('active','defeated')),
    respawn_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(server_id, x, y)
);
