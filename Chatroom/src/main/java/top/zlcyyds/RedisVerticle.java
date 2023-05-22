package top.zlcyyds;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.redis.client.Response;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import top.zlcyyds.Util.RedisUtil;
import top.zlcyyds.Util.mysqlUtil;

import java.util.*;

public class RedisVerticle extends AbstractVerticle {
    @Override
    public void start() throws Exception {

       Server.router.route("/showfriends").handler(BodyHandler.create()).handler(this::showFriends);
        Server.router.route("/deletefriends").handler(BodyHandler.create()).handler(this::deleteFriends);

    }

    /**
     * 在redis中查询好友列表
     * @param routingContext
     */
    private void showFriends(RoutingContext routingContext) {

        RedisUtil.getRedisClient(Server.vertx)
                .smembers(routingContext.request().getParam("myself"),rs->{
                    if(rs.succeeded()){
                        Response response = rs.result();
                        String membersStr = response.toString();
                        System.out.println(membersStr);
                        routingContext.response().end(new JsonObject().put("code", "200").put("data", membersStr).encode());
//                        JsonArray members = new JsonArray(membersStr);
//
//
//                        System.out.println("Members of set: " + members.encode());
                    }
                    else{
                        routingContext.response().end(new JsonObject().put("code", "100").put("error", "系统错误！！").encode());

                    }
                });
    }

    /**
     * 在redis中删除好友
     */

    private void deleteFriends(RoutingContext routingContext){


        mysqlUtil.client(Server.vertx)
                .preparedQuery("SELECT username FROM user WHERE  userid=?")
                .execute(Tuple.of(routingContext.request().getParam("userid")))
                .onComplete(rs->{
                    if (rs.failed()) {
                        routingContext.response().end(new JsonObject().put("code", "100").put("error", "发生未知错误！").encode());
                        mysqlUtil.client(vertx).close();
                    }
                    if (rs.succeeded()) {

                        RowSet<Row> result = rs.result();


                }
                    }).onComplete(rs->{
            RowSet<Row> result = rs.result();
                        String username=null;
                        for (Row row : result) {
                            username =(String) row.getValue("username");
                            routingContext.put("username",username);

        }


      String id=  routingContext.request().getParam("userid")+"-"+username;

            List<String> list = Arrays.asList(routingContext.request().getParam("myself"), id);
            RedisUtil.getRedisClient(Server.vertx)
                .srem(list,rw->{
                    if(rs.succeeded()) {
                        Response response = rw.result();
                        String membersStr = response.toString();
                        routingContext.response().end(new JsonObject().put("code", "200").put("data", "成功删除"+membersStr+"条数据").encode());
                    }
                    else{
                        routingContext.response().end(new JsonObject().put("code", "100").put("error", "系统错误！！").encode());

                    }
                });
        });
    }
}
