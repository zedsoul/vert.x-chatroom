package top.zlcyyds.Util;

import io.vertx.core.Vertx;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;

public class mysqlUtil {
 private  static    MySQLConnectOptions connectOptions = new MySQLConnectOptions()
            .setPort(3306)
            .setHost("162.14.73.229")
            .setDatabase("chatroom")
            .setUser("zlc_1")
            .setPassword("6czmYbhmHzrFPXte");


    // 连接池选项
   private static PoolOptions poolOptions = new PoolOptions()
            .setMaxSize(5);

    // 创建客户端池
    public  static  MySQLPool client(Vertx vertx) {
        Object o = new Object();

        MySQLPool client = MySQLPool.pool(vertx, connectOptions, poolOptions);
        return client;
    }
}
