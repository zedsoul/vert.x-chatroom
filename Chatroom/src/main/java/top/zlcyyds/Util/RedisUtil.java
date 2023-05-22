package top.zlcyyds.Util;

import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.impl.RedisClient;

public class RedisUtil {
    private static final RedisOptions options = new RedisOptions();

    static {

    }

    public static RedisAPI getRedisClient(Vertx vertx) {

        RedisClient client =(RedisClient) Redis.createClient(vertx, "redis://162.14.73.229:6379");
        RedisAPI api = RedisAPI.api(client);
        return api;
    }
}

