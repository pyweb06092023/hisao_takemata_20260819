import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/*
クラス名      : App
機能          : Todoアプリケーションの起動およびHTTPリクエスト処理
概要          : com.sun.net.httpserver を利用して、Todoの追加・一覧・完了・未完了・削除を行う。
作成者        : 要確認
作成日        : 要確認
更新履歴      : 2026-08-20 コメント追加
                2026-08-20 文字化け修正
*/
public class App {
    static List<Todo> todos = new ArrayList<>();
    static int nextId = 1;
    static final int MAX_TODO_LENGTH = 80;

    /*
    メソッド名    : main
    機能          : アプリケーションを起動する
    引数          : String[] args - 起動引数（要確認）
    戻り値        : なし
    例外          : Exception
    処理概要      : 初期データを登録し、HTTPサーバをポート8080で起動する。
                    受信したパスに応じてTodoの追加・一覧・完了・未完了・削除を処理する。
    */
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

            if (path.equals("/add") && (method.equals("POST") || method.equals("GET"))) {
                String value = "";
                if (method.equals("POST")) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    if (body.startsWith("todo=") && body.length() > 5) {
                        value = body.substring(5);
                    }
                } else {
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null && query.startsWith("todo=") && query.length() > 5) {
                        value = query.substring(5);
                    }
                }

                String title = URLDecoder.decode(value, StandardCharsets.UTF_8);
                if (title.length() > MAX_TODO_LENGTH) {
                    String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Location", "/?error=too_long&todo=" + encodedTitle);
                    exchange.sendResponseHeaders(303, -1);
                    exchange.close();
                    return;
                }

                if (!title.isEmpty()) {
                    todos.add(new Todo(nextId, title));
                    nextId++;
                }

                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            } else if (path.equals("/done")) {
                String query = exchange.getRequestURI().getQuery();
                Integer id = parseId(query);
                if (id == null) {
                    exchange.getResponseHeaders().set("Location", "/");
                    exchange.sendResponseHeaders(303, -1);
                    exchange.close();
                    return;
                }

                for (Todo todo : todos) {
                    if (todo.getId() == id) {
                        todo.setDone(true);
                    }
                }

                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            } else if (path.equals("/undo")) {
                String query = exchange.getRequestURI().getQuery();
                Integer id = parseId(query);
                if (id == null) {
                    exchange.getResponseHeaders().set("Location", "/");
                    exchange.sendResponseHeaders(303, -1);
                    exchange.close();
                    return;
                }

                for (Todo todo : todos) {
                    if (todo.getId() == id) {
                        todo.setDone(false);
                    }
                }

                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            } else if (path.equals("/delete")) {
                String query = exchange.getRequestURI().getQuery();
                Integer id = parseId(query);
                if (id == null) {
                    exchange.getResponseHeaders().set("Location", "/");
                    exchange.sendResponseHeaders(303, -1);
                    exchange.close();
                    return;
                }

                todos.removeIf(todo -> todo.getId() == id);

                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            } else if (path.equals("/")) {
                String query = exchange.getRequestURI().getQuery();
                String errorMessage = "";
                String inputValue = "";
                if (query != null) {
                    for (String part : query.split("&")) {
                        if (part.equals("error=too_long")) {
                            errorMessage = "80文字までが、入力可能です。";
                        } else if (part.startsWith("todo=") && part.length() > 5) {
                            inputValue = URLDecoder.decode(part.substring(5), StandardCharsets.UTF_8);
                        }
                    }
                }

                String html = "<!doctype html>"
                        + "<html><head><meta charset='UTF-8'><title>わたしのTodo</title>"
                        + "<style>"
                        + "body{max-width:640px;margin:24px auto;padding:0 16px;font-size:18px;line-height:1.6;}"
                        + "h1{font-size:32px;margin-bottom:16px;}"
                        + "form{margin-bottom:16px;}"
                        + "input,button{font-size:18px;}"
                        + "ul{padding-left:24px;}"
                        + "p.error{color:#c62828;margin:0 0 16px;}"
                        + "</style></head><body>"
                        + "<h1>わたしのTodo</h1>"
                        + "<form method='post' action='/add'>"
                        + "<input name='todo' value='" + escapeHtml(inputValue) + "'>"
                        + "<button>追加</button></form>";

                if (!errorMessage.isEmpty()) {
                    html += "<p class='error'>" + errorMessage + "</p>";
                }

                if (todos.isEmpty()) {
                    html += "<p>やることは、いまゼロです。</p>";
                } else {
                    html += "<ul>";
                    for (Todo todo : todos) {
                        String mark = "";
                        String actionLink = " <a href='/done?id=" + todo.getId() + "'>完了</a>";
                        if (todo.isDone()) {
                            mark = " ✓";
                            actionLink = " <a href='/undo?id=" + todo.getId() + "'>未完了</a>";
                        }
                        html += "<li>" + escapeHtml(todo.getTitle()) + mark
                                + actionLink
                                + " <a href='/delete?id=" + todo.getId() + "'>削除</a></li>";
                    }
                    html += "</ul>";
                }

                html += "</body></html>";
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

    /*
    メソッド名    : parseId
    機能          : クエリ文字列からIDを取得する
    引数          : String query - リクエストのクエリ文字列
    戻り値        : Integer - 取得したID。取得できない場合は null
    例外          : なし
    処理概要      : query が id= で始まるかを確認し、数値に変換して返す。
                    数値変換できない場合や形式不正の場合は null を返す。
    */
    static Integer parseId(String query) {
        if (query == null || !query.startsWith("id=") || query.length() <= 3) {
            return null;
        }

        try {
            return Integer.parseInt(query.substring(3));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /*
    メソッド名    : escapeHtml
    機能          : HTML特殊文字をエスケープする
    引数          : String text - HTML表示用に変換する文字列
    戻り値        : String - エスケープ後の文字列
    例外          : なし
    処理概要      : &, <, >, " , ' を HTMLエンティティへ置換し、
                    ブラウザ表示時の文字崩れや意図しない解釈を抑止する。
    */
    static String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

/*
クラス名      : Todo
機能          : Todo情報の保持
概要          : 1件分のTodoについて、ID、タイトル、完了状態を保持する。
作成者        : 要確認
作成日        : 要確認
更新履歴      : 2026-08-20 コメント追加
                2026-08-20 文字化け修正
*/
class Todo {
    private final int id;
    private final String title;
    private boolean done;

    /*
    メソッド名    : Todo
    機能          : Todoオブジェクトを生成する
    引数          : int id - Todoの識別子
                    String title - Todoのタイトル
    戻り値        : なし
    例外          : なし
    処理概要      : IDとタイトルを設定し、完了状態を未完了(false)で初期化する。
    */
    Todo(int id, String title) {
        this.id = id;
        this.title = title;
        this.done = false;
    }

    /*
    メソッド名    : getId
    機能          : TodoのIDを取得する
    引数          : なし
    戻り値        : int - TodoのID
    例外          : なし
    処理概要      : 保持しているIDを返す。
    */
    int getId() {
        return id;
    }

    /*
    メソッド名    : getTitle
    機能          : Todoのタイトルを取得する
    引数          : なし
    戻り値        : String - Todoのタイトル
    例外          : なし
    処理概要      : 保持しているタイトルを返す。
    */
    String getTitle() {
        return title;
    }

    /*
    メソッド名    : isDone
    機能          : Todoの完了状態を取得する
    引数          : なし
    戻り値        : boolean - 完了状態。完了時 true、未完了時 false
    例外          : なし
    処理概要      : 保持している完了状態を返す。
    */
    boolean isDone() {
        return done;
    }

    /*
    メソッド名    : setDone
    機能          : Todoの完了状態を設定する
    引数          : boolean done - 設定する完了状態
    戻り値        : なし
    例外          : なし
    処理概要      : 引数で受け取った値を完了状態として設定する。
    */
    void setDone(boolean done) {
        this.done = done;
    }
}
