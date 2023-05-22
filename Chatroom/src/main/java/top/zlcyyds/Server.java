package top.zlcyyds;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;
import io.vertx.sqlclient.*;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.util.ByteSource;
import top.zlcyyds.Handler.messageHandler;
import top.zlcyyds.Util.RedisUtil;
import top.zlcyyds.Util.mysqlUtil;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Server extends AbstractVerticle {

    Logger logger = LoggerFactory.getLogger(Server.class);
    public static Vertx vertx=Vertx.vertx();
    public static Router router=Router.router(vertx);


    public static void main(String[] args) {


        vertx.deployVerticle(new Server());
        vertx.deployVerticle(new RedisVerticle());
        System.out.println("start~!");
    }

    public void start() {

        HttpServer server = vertx.createHttpServer();


        PermittedOptions inboundPermitted1 = new PermittedOptions()
                .setAddress("chatroom");

        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        SockJSBridgeOptions options = new SockJSBridgeOptions()
                .addInboundPermitted(inboundPermitted1)
                .addOutboundPermitted(inboundPermitted1);
//设置跨域
        CorsHandler corsHandler = CorsHandler.create("*")
                .allowedMethod(io.vertx.core.http.HttpMethod.GET)
                .allowedMethod(io.vertx.core.http.HttpMethod.POST)
                .allowedHeader("Content-Type");
        router.route().handler(corsHandler);
        router
                .route("/eventbus/*")
                .subRouter(sockJSHandler.bridge(options));

        vertx.eventBus().consumer("chatroom", message -> {
            System.out.println("Received message on 'chatroom': " + message.body());
        });

        router.route().handler(StaticHandler.create());
/**
 * 注册路由
 */
        router.route(HttpMethod.POST,"/register")
                .handler(BodyHandler.create())
                .handler(this::register);

/**
 * login路由
 */
        router.route(HttpMethod.POST,"/login").handler(this::JWTHandler);


        router.route(HttpMethod.POST,"/addFriends").handler(BodyHandler.create()).handler(this::RedisHandler);
        server.requestHandler(router).listen(38080);
    }

    private void register(RoutingContext rct) {
        String userid = rct.request().getParam("userid");
        if(userid.length()<8){
            rct.response().end(new JsonObject().put("code",100).put("data","您的ID长度小于八位！").encode());
            return;
        }
        String password = rct.request().getParam("password");
        String salt="dasldjalskjfgvlanclja23lasjdla ";
        String code = md5_salt(password, salt);
       UserCount(rct, userid,code);



    }


    /**
     * JWT
     */
    private void JWTHandler(RoutingContext ctx) {
        JWTOptions jwtOptions = new JWTOptions();
        jwtOptions.setExpiresInMinutes(30);
        JWTAuthOptions authConfig = new JWTAuthOptions().setJWTOptions(jwtOptions);

        JWTAuth jwt = JWTAuth.create(vertx, authConfig);
            ctx.response().putHeader("content-type","application/json");
            //这是个例子，身份认证应当使用其他的提供者
            if (
                    "paulo".equals(ctx.request().getParam("username")) &&
                            "secret".equals(ctx.request().getParam("password"))) {
                ctx.response()
                        .end(new JsonObject().put("code",200).put("token",jwt.generateToken(new JsonObject().put("username", "paulo"))).encode());
            } else {
                ctx.fail(401);
            }
        }

/**
 * mysql
 *
 * @return
 */
private static void UserCount(RoutingContext rct,String userid,String password){
// 一个简单的查询
  mysqlUtil.client(vertx)
            .preparedQuery("SELECT * FROM user WHERE  userid=?")
            .execute(Tuple.of(userid),ar -> {
                if (ar.succeeded()) {
                    RowSet<Row> result = ar.result();
                    System.out.println(result.size());
                    if(result.size()>0) {
                        rct.response().end(new JsonObject().put("code",100).put("data","该id已经被注册过了,换一个试试呗").encode());
                        System.out.println("there is have:" + result.size() + " users ");
                    }
                    else if(result.size()==0){

                        mysqlUtil.client(vertx).getConnection(conn->{
                            if (conn.succeeded()){
                                SqlConnection connection = conn.result();
                                connection
                                        .preparedQuery("insert  into user(userid,password,role )values (?,?,?)")
                                        .execute(Tuple.of(userid,password,0),br-> {
                                            if (br.succeeded()) {

                                                RowSet<Row> result1 = br.result();

                                                if (result1.rowCount() > 0) {
                                                     connection.close();
                                                    rct.response().end(new JsonObject().put("code", 200).put("data", "注册成功").encode());
                                                }
                                            }
                                            if (br.failed()) {
                                                connection.close();
                                                System.out.println(br.cause().getMessage());
                                                rct.response().end(new JsonObject().put("code", 100).put("data", "插入失败了！").encode());
                                                mysqlUtil.client(vertx).close();
                                            }
                                        });
                                        }
                        });



                    }
                }




            }
            );

}
    private  static String md5_salt(String password, String salt) {
        //加密方式
        String hashAlgorithmName = "MD5";
        //盐：相同密码使用不同的盐加密后的结果不同
        ByteSource byteSalt = ByteSource.Util.bytes(salt);
        //密码
        Object source = password;
        //加密次数
        int hashIterations = 2;
        SimpleHash result = new SimpleHash(hashAlgorithmName, source, byteSalt, hashIterations);
        return result.toString();
    }



/**
 * Redist添加好友
 * param: myself(自己的id) ，userid(好友的id)
 */
    public void RedisHandler(RoutingContext rtc) {

        Future<RowSet<Row>> future = mysqlUtil.client(vertx)
                .preparedQuery("SELECT * FROM user WHERE  userid=?")
                .execute(Tuple.of(rtc.request().getParam("userid")))
                .onComplete(rs -> {
                    if (rs.failed()) {
                        rtc.response().end(new JsonObject().put("code", "100").put("error", "发生未知错误！").encode());
                        mysqlUtil.client(vertx).close();
                    }
                    if (rs.succeeded()) {

                        RowSet<Row> result = rs.result();
                        String username=null;
                        for (Row row : result) {
                            username =(String) row.getValue("username");
                        }
                        System.out.println("username:"+username);
                        String useridName=rtc.request().getParam("userid")+"-"+username;
                        int i = result.size();
                        logger.info("i:"+i);
                        if (i != 0) {

                            mysqlUtil.client(vertx).close();
                            List<String> friendsList = Arrays.asList(rtc.request().getParam("myself"), useridName);
                            RedisAPI redisClient = RedisUtil.getRedisClient(vertx);
                            // TODO 判断redis里面是否存在该好友 虽然也存不进去
//                            Response userid = redisClient.smembers(rtc.request().getParam("userid"))
//                                    .result();
//                            System.out.println("COMHE"+userid.toString());
//                            if (userid.toString().equals("0")) {
                                redisClient.sadd(friendsList)
                                        .onComplete(res -> {
                                                    if (res.succeeded()) {
                                                        Response response = res.result();
                                                        rtc.response().end(new JsonObject().put("code", "200").put("success", "添加好友成功,如果重复添加会添加不上的哦！！").encode());
                                                    } else {
                                                        logger.info("put failed:" + res.cause().getMessage());
                                                    }
                                                }
                                        );
                            }
//                        }
                        else if(i==0){
                            rtc.response().end(new JsonObject().put("code", "100").put("error", "该用户还未注册！！").encode());
                        }
                    }

                }

                );




    }
}
