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

            if (path.equals("/add") && (method.equals("POST") || method.equals("GET"))) {
                String value = "";
                if (method.equals("POST")) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    value = body.substring(5);
                } else {
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null && query.startsWith("todo=") && query.length() > 5) {
                        value = query.substring(5);
                    }
                }

                String title = URLDecoder.decode(value, StandardCharsets.UTF_8);
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
                String html = "<!doctype html>"
                        + "<html><head><meta charset='UTF-8'><title>わたしのTodo</title>"
                        + "<style>"
                        + "body{max-width:640px;margin:24px auto;padding:0 16px;font-size:18px;line-height:1.6;}"
                        + "h1{font-size:32px;margin-bottom:16px;}"
                        + "form{margin-bottom:16px;}"
                        + "input,button{font-size:18px;}"
                        + "ul{padding-left:24px;}"
                        + "</style></head><body>"
                        + "<h1>わたしのTodo</h1>"
                        + "<form method='post' action='/add'>"
                        + "<input name='todo'><button>追加</button></form>";

                if (todos.isEmpty()) {
                    html += "<p>やることは、いまゼロです</p>";
                } else {
                    html += "<ul>";
                    for (Todo todo : todos) {
                        String mark = "";
                        if (todo.isDone()) {
                            mark = " ✓";
                        }
                        html += "<li>" + todo.getTitle() + mark
                                + " <a href='/done?id=" + todo.getId() + "'>完了</a>"
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
}

class Todo {
    private final int id;
    private final String title;
    private boolean done;

    Todo(int id, String title) {
        this.id = id;
        this.title = title;
        this.done = false;
    }

    int getId() {
        return id;
    }

    String getTitle() {
        return title;
    }

    boolean isDone() {
        return done;
    }

    void setDone(boolean done) {
        this.done = done;
    }
}
