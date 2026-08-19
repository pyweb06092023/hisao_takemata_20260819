/**
 * Todo サーバーのサンプルです。
 *
 * できること:
 * 1. 一覧表示
 * 2. Todo の追加
 * 3. Todo を完了にする
 * 4. Todo を削除する
 *
 * URL の仕様:
 * 1. GET /
 *    一覧画面を表示します。
 * 2. POST /add
 *    フォームから Todo を追加します。
 * 3. GET /add?todo=TEST
 *    URL から Todo を追加します。
 * 4. GET /done?id=2
 *    id が一致する 1 件を完了にします。
 * 5. GET /delete?id=2
 *    id が一致する 1 件を削除します。
 *
 * HTML から URL を使って追加する例:
 * <a href="http://localhost:8080/add?todo=TEST">TEST を追加</a>
 *
 * 起動方法:
 * 1. javac App.java
 * 2. java App
 * 3. ブラウザで http://localhost:8080/ を開く
 */
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class App {
    static List<Todo> todos = new ArrayList<>();
    static int nextId = 1;

    public static void main(String[] args) throws Exception {
        todos.add(new Todo(nextId++, "牛乳を買う"));
        Todo egg = new Todo(nextId++, "卵を買う");
        egg.setDone(true);
        todos.add(egg);

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String message;
            String method = exchange.getRequestMethod();
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");

            if (path.equals("/add") && (method.equals("POST") || method.equals("GET"))) { // ★追加
                String value = ""; // ★追加
                if (method.equals("POST")) { // ★追加
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    value = body.substring(5);
                } else { // ★追加
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null && query.startsWith("todo=") && query.length() > 5) { // ★追加
                        value = query.substring(5); // ★追加
                    } // ★追加
                } // ★追加
                String title = URLDecoder.decode(value, StandardCharsets.UTF_8);
                if (!title.isEmpty()) {
                    todos.add(new Todo(nextId, title));
                    nextId++;
                }
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            } else if (path.equals("/done")) { // ★追加
                String query = exchange.getRequestURI().getQuery(); // ★追加
                Integer id = parseId(query); // ★追加
                if (id == null) { // ★追加
                    exchange.getResponseHeaders().set("Location", "/"); // ★追加
                    exchange.sendResponseHeaders(303, -1); // ★追加
                    exchange.close(); // ★追加
                    return; // ★追加
                } // ★追加

                for (Todo todo : todos) { // ★追加
                    if (todo.getId() == id) { // ★追加
                        todo.setDone(true); // ★追加
                    } // ★追加
                } // ★追加

                exchange.getResponseHeaders().set("Location", "/"); // ★追加
                exchange.sendResponseHeaders(303, -1); // ★追加
                exchange.close(); // ★追加
                return; // ★追加
            } else if (path.equals("/delete")) { // ★追加
                String query = exchange.getRequestURI().getQuery(); // ★追加
                Integer id = parseId(query); // ★追加
                if (id == null) { // ★追加
                    exchange.getResponseHeaders().set("Location", "/"); // ★追加
                    exchange.sendResponseHeaders(303, -1); // ★追加
                    exchange.close(); // ★追加
                    return; // ★追加
                } // ★追加

                todos.removeIf(todo -> todo.getId() == id); // ★追加

                exchange.getResponseHeaders().set("Location", "/"); // ★追加
                exchange.sendResponseHeaders(303, -1); // ★追加
                exchange.close(); // ★追加
                return; // ★追加
            } else if (path.equals("/")) {
                String html = "<form method='post' action='/add'>"
                        + "<input name='todo'><button>追加</button></form>"
                        + "<ul>";
                for (Todo todo : todos) {
                    String mark = "";
                    if (todo.isDone()) {
                        mark = " ✔";
                    }
                    html += "<li>" + todo.getTitle() + mark
                            + " <a href='/done?id=" + todo.getId() + "'>完了</a>" // ★追加
                            + " <a href='/delete?id=" + todo.getId() + "'>削除</a></li>"; // ★追加
                }
                html += "</ul>";
                message = html;
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            } else {
                message = "ページが見つかりません";
            }
            byte[] responseBody = message.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.getResponseBody().close();
        });

        server.start();
        System.out.println("サーバー起動: http://localhost:8080 (止めるときは Ctrl+C)");
    }

    static Integer parseId(String query) { // ★追加
        if (query == null || !query.startsWith("id=") || query.length() <= 3) { // ★追加
            return null; // ★追加
        } // ★追加
        try { // ★追加
            return Integer.parseInt(query.substring(3)); // ★追加
        } catch (NumberFormatException e) { // ★追加
            return null; // ★追加
        } // ★追加
    } // ★追加
}

/**
 * Todo を表すクラス。
 * 1件分の id、タイトル、完了状態を持つ。
 */
class Todo {
    private final int id;
    private final String title;
    private boolean done;

    /**
     * 新しい Todo を作る。
     * done は最初は false で始まる。
     *
     * @param id Todo を見分ける番号
     * @param title Todo の内容
     */
    Todo(int id, String title) {
        this.id = id;
        this.title = title;
        this.done = false;
    }

    /**
     * Todo の id を返す。
     *
     * @return Todo の id
     */
    int getId() {
        return id;
    }

    /**
     * Todo のタイトルを返す。
     *
     * @return Todo のタイトル
     */
    String getTitle() {
        return title;
    }

    /**
     * Todo が完了済みかどうかを返す。
     *
     * @return 完了済みなら true、未完了なら false
     */
    boolean isDone() {
        return done;
    }

    /**
     * Todo の完了状態を変更する。
     *
     * @param done 設定したい完了状態
     */
    void setDone(boolean done) {
        this.done = done;
    }
}
