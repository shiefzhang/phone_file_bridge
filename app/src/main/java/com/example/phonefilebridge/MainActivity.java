package com.example.phonefilebridge;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {
    private static final int DEFAULT_PORT = 8080;
    private static final String PREFS = "phone_file_bridge";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_PORT = "port";

    private TextView statusText;
    private TextView linkText;
    private EditText passwordEdit;
    private EditText portEdit;
    private FileHttpServer server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        requestStorageAccess();
        startServer();
    }

    @Override
    protected void onDestroy() {
        if (server != null) {
            server.stop();
        }
        super.onDestroy();
    }

    private void buildUi() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#EEF4F8"));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(28));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("\u624b\u673a\u6587\u4ef6\u6865");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#102033"));
        root.addView(title);

        TextView description = new TextView(this);
        description.setText("\u624b\u673a\u548c\u7535\u8111\u8fde\u63a5\u540c\u4e00\u4e2a\u5c40\u57df\u7f51\u540e\uff0c\u5728\u7535\u8111\u6d4f\u89c8\u5668\u6253\u5f00\u4e0b\u9762\u7684\u5730\u5740\u5373\u53ef\u7ba1\u7406\u6587\u4ef6\u3002");
        description.setTextSize(15);
        description.setTextColor(Color.parseColor("#4F6075"));
        description.setPadding(0, dp(12), 0, dp(16));
        root.addView(description);

        statusText = new TextView(this);
        statusText.setTextColor(Color.parseColor("#0F7A55"));
        statusText.setTextSize(16);
        statusText.setPadding(0, dp(10), 0, dp(6));
        root.addView(statusText);

        linkText = new TextView(this);
        linkText.setTextSize(20);
        linkText.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        linkText.setTextColor(Color.parseColor("#1459B8"));
        linkText.setBackground(panelBackground("#FFFFFF", "#CAD7E5", 12));
        linkText.setTextIsSelectable(true);
        linkText.setPadding(dp(14), dp(12), dp(14), dp(12));
        root.addView(linkText);

        TextView passwordLabel = new TextView(this);
        passwordLabel.setText("\u8bbf\u95ee\u5bc6\u7801");
        passwordLabel.setTextSize(16);
        passwordLabel.setTypeface(Typeface.DEFAULT_BOLD);
        passwordLabel.setTextColor(Color.parseColor("#1B2A3D"));
        passwordLabel.setPadding(0, dp(18), 0, dp(6));
        root.addView(passwordLabel);

        passwordEdit = new EditText(this);
        passwordEdit.setSingleLine(true);
        passwordEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordEdit.setText(prefs.getString(KEY_PASSWORD, ""));
        passwordEdit.setBackground(panelBackground("#FFFFFF", "#CAD7E5", 10));
        passwordEdit.setPadding(dp(12), 0, dp(12), 0);
        passwordEdit.setHint("\u5efa\u8bae\u8bbe\u7f6e\u4e00\u4e2a\u8bbf\u95ee\u5bc6\u7801");
        root.addView(passwordEdit);

        Switch showPassword = new Switch(this);
        showPassword.setTextColor(Color.parseColor("#324359"));
        showPassword.setText("\u663e\u793a\u5bc6\u7801");
        showPassword.setPadding(0, dp(8), 0, dp(8));
        showPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                passwordEdit.setInputType(InputType.TYPE_CLASS_TEXT
                        | (isChecked ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD));
                passwordEdit.setSelection(passwordEdit.getText().length());
            }
        });
        root.addView(showPassword);

        TextView portLabel = new TextView(this);
        portLabel.setText("\u7aef\u53e3\u53f7");
        portLabel.setTextSize(16);
        portLabel.setTypeface(Typeface.DEFAULT_BOLD);
        portLabel.setTextColor(Color.parseColor("#1B2A3D"));
        portLabel.setPadding(0, dp(12), 0, dp(6));
        root.addView(portLabel);

        portEdit = new EditText(this);
        portEdit.setSingleLine(true);
        portEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
        portEdit.setText(String.valueOf(prefs.getInt(KEY_PORT, DEFAULT_PORT)));
        portEdit.setHint("1024-65535");
        portEdit.setBackground(panelBackground("#FFFFFF", "#CAD7E5", 10));
        portEdit.setPadding(dp(12), 0, dp(12), 0);
        root.addView(portEdit);

        Button saveButton = new Button(this);
        saveButton.setText("\u4fdd\u5b58\u8bbe\u7f6e\u5e76\u91cd\u542f\u670d\u52a1");
        saveButton.setAllCaps(false);
        saveButton.setGravity(Gravity.CENTER);
        saveButton.setTextColor(Color.WHITE);
        saveButton.setBackground(panelBackground("#1459B8", "#1459B8", 10));
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int port = parsePort(portEdit.getText().toString());
                if (port < 0) {
                    statusText.setTextColor(Color.parseColor("#B91C1C"));
                    statusText.setText("\u7aef\u53e3\u53f7\u8bf7\u8f93\u5165 1024-65535");
                    return;
                }
                prefs.edit()
                        .putString(KEY_PASSWORD, passwordEdit.getText().toString())
                        .putInt(KEY_PORT, port)
                        .apply();
                startServer();
                Toast.makeText(MainActivity.this, "\u5df2\u4fdd\u5b58", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(saveButton);

        Button permissionButton = new Button(this);
        permissionButton.setText("\u6253\u5f00\u6587\u4ef6\u8bbf\u95ee\u6743\u9650\u8bbe\u7f6e");
        permissionButton.setAllCaps(false);
        permissionButton.setTextColor(Color.parseColor("#1459B8"));
        permissionButton.setBackground(panelBackground("#FFFFFF", "#CAD7E5", 10));
        permissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openStorageSettings();
            }
        });
        root.addView(permissionButton);

        TextView note = new TextView(this);
        note.setText("\u9ed8\u8ba4\u6253\u5f00\u516c\u5171 Download \u76ee\u5f55\uff0c\u66f4\u65b9\u4fbf\u5176\u4ed6\u5e94\u7528\u8bfb\u5199\u548c\u67e5\u770b\u3002Android 11 \u53ca\u4ee5\u4e0a\u9700\u8981\u5141\u8bb8\u201c\u7ba1\u7406\u6240\u6709\u6587\u4ef6\u201d\uff0c\u5426\u5219\u53ef\u80fd\u53ea\u80fd\u8bbf\u95ee\u90e8\u5206\u6587\u4ef6\u3002");
        note.setTextSize(13);
        note.setTextColor(Color.parseColor("#5F6F84"));
        note.setPadding(0, dp(16), 0, 0);
        root.addView(note);

        setContentView(scroll);
    }

    private void startServer() {
        if (server != null) {
            server.stop();
        }
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String password = prefs.getString(KEY_PASSWORD, "");
        int port = prefs.getInt(KEY_PORT, DEFAULT_PORT);
        server = new FileHttpServer(port, password);
        try {
            server.start();
            String url = "http://" + getLocalIpAddress() + ":" + port;
            statusText.setTextColor(Color.parseColor("#0F7A55"));
            statusText.setText("\u670d\u52a1\u5df2\u5f00\u542f\uff0c\u7aef\u53e3 " + port);
            linkText.setText(url);
        } catch (IOException e) {
            server = null;
            statusText.setTextColor(Color.parseColor("#B91C1C"));
            statusText.setText("\u670d\u52a1\u542f\u52a8\u5931\u8d25\uff1a" + e.getMessage());
            linkText.setText("\u8bf7\u66f4\u6362\u7aef\u53e3\u540e\u91cd\u8bd5");
        }
    }

    private int parsePort(String value) {
        try {
            int port = Integer.parseInt(value.trim());
            return port >= 1024 && port <= 65535 ? port : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private void requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            openStorageSettings();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> permissions = new ArrayList<>();
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                    && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (!permissions.isEmpty()) {
                requestPermissions(permissions.toArray(new String[0]), 10);
            }
        }
    }

    private void openStorageSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
        }
    }

    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                Enumeration<java.net.InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "127.0.0.1";
    }

    private GradientDrawable panelBackground(String fill, String stroke, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(fill));
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), Color.parseColor(stroke));
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class FileHttpServer {
        private final int port;
        private final String password;
        private final File rootDir;
        private final String defaultPath;
        private final ExecutorService workers = Executors.newCachedThreadPool();
        private final Map<String, Long> sessions = new ConcurrentHashMap<>();
        private final SecureRandom random = new SecureRandom();
        private volatile boolean running;
        private ServerSocket serverSocket;

        FileHttpServer(int port, String password) {
            this.port = port;
            this.password = password == null ? "" : password;
            this.rootDir = Environment.getExternalStorageDirectory();
            File downloadDir = getSharedDownloadDirectory();
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }
            this.defaultPath = relativePathQuietly(downloadDir, Environment.DIRECTORY_DOWNLOADS);
        }

        void start() throws IOException {
            running = true;
            serverSocket = new ServerSocket(port);
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    serve();
                }
            }, "file-http-server");
            thread.start();
        }

        void stop() {
            running = false;
            closeQuietly(serverSocket);
            workers.shutdownNow();
        }

        private void serve() {
            try {
                while (running) {
                    final Socket socket = serverSocket.accept();
                    workers.execute(new Runnable() {
                        @Override
                        public void run() {
                            handle(socket);
                        }
                    });
                }
            } catch (IOException ignored) {
            }
        }

        private void handle(Socket socket) {
            try {
                socket.setSoTimeout(30000);
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();
                Request request = readRequest(in);
                if (request == null) {
                    return;
                }

                if ("/login".equals(request.path) && "POST".equals(request.method)) {
                    handleLogin(request, in, out);
                } else if (!isAuthorized(request)) {
                    sendLogin(out, false);
                } else if ("/download".equals(request.path)) {
                    sendDownload(out, request.query.get("path"));
                } else if ("/download_zip".equals(request.path)) {
                    String paths = request.query.get("paths");
                    if ("POST".equals(request.method)) {
                        byte[] body = readBytes(in, request.contentLength);
                        paths = parseQuery(new String(body, StandardCharsets.UTF_8)).get("paths");
                    }
                    sendZipDownload(out, paths);
                } else if ("/upload_raw".equals(request.path) && "POST".equals(request.method)) {
                    handleUpload(request, in, out);
                } else {
                    sendDirectory(out, request.query.get("path"), "");
                }
            } catch (Exception e) {
                try {
                    sendText(socket.getOutputStream(), 500, "text/plain; charset=utf-8", "Server error: " + e.getMessage());
                } catch (Exception ignored) {
                }
            } finally {
                closeQuietly(socket);
            }
        }

        private Request readRequest(InputStream in) throws IOException {
            ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
            int matched = 0;
            int b;
            byte[] end = new byte[]{'\r', '\n', '\r', '\n'};
            while ((b = in.read()) != -1) {
                headerBuffer.write(b);
                matched = b == end[matched] ? matched + 1 : (b == end[0] ? 1 : 0);
                if (matched == end.length) {
                    break;
                }
                if (headerBuffer.size() > 32768) {
                    throw new IOException("Request headers are too large");
                }
            }
            if (headerBuffer.size() == 0) {
                return null;
            }
            String headersText = headerBuffer.toString("UTF-8");
            String[] lines = headersText.split("\r\n");
            if (lines.length == 0) {
                return null;
            }
            String[] first = lines[0].split(" ");
            if (first.length < 2) {
                return null;
            }
            Request request = new Request();
            request.method = first[0];
            String target = first[1];
            int q = target.indexOf('?');
            request.path = q >= 0 ? target.substring(0, q) : target;
            request.query = parseQuery(q >= 0 ? target.substring(q + 1) : "");
            request.headers = new HashMap<>();
            for (int i = 1; i < lines.length; i++) {
                int colon = lines[i].indexOf(':');
                if (colon > 0) {
                    request.headers.put(lines[i].substring(0, colon).trim().toLowerCase(Locale.US),
                            lines[i].substring(colon + 1).trim());
                }
            }
            request.contentLength = parseLong(request.headers.get("content-length"), 0);
            return request;
        }

        private void handleLogin(Request request, InputStream in, OutputStream out) throws IOException {
            byte[] body = readBytes(in, Math.min(request.contentLength, 8192));
            Map<String, String> form = parseQuery(new String(body, StandardCharsets.UTF_8));
            String submitted = form.get("password");
            if (password.equals(submitted == null ? "" : submitted)) {
                String token = newToken();
                sessions.put(token, System.currentTimeMillis());
                sendRedirect(out, "/", "Set-Cookie: session=" + token + "; Path=/; HttpOnly\r\n");
            } else {
                sendLogin(out, true);
            }
        }

        private boolean isAuthorized(Request request) {
            if (password.isEmpty()) {
                return true;
            }
            String cookie = request.headers.get("cookie");
            if (cookie == null) {
                return false;
            }
            String[] parts = cookie.split(";");
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.startsWith("session=")) {
                    String token = trimmed.substring("session=".length());
                    Long created = sessions.get(token);
                    return created != null && System.currentTimeMillis() - created < 24L * 60L * 60L * 1000L;
                }
            }
            return false;
        }

        private void sendLogin(OutputStream out, boolean failed) throws IOException {
            String html = page("Login",
                    "<main class=\"login\">"
                            + "<h1>Phone File Bridge</h1>"
                            + (failed ? "<p class=\"error\">Password is incorrect.</p>" : "")
                            + "<form method=\"post\" action=\"/login\">"
                            + "<input type=\"password\" name=\"password\" placeholder=\"Password\" autofocus>"
                            + "<button type=\"submit\">Login</button>"
                            + "</form></main>");
            sendText(out, 200, "text/html; charset=utf-8", html);
        }

        private void sendDirectory(OutputStream out, String requestedPath, String message) throws IOException {
            File dir = resolvePath(requestedPath);
            if (!dir.exists() || !dir.isDirectory()) {
                sendText(out, 404, "text/plain; charset=utf-8", "Directory not found");
                return;
            }
            File[] files = dir.listFiles();
            if (files == null) {
                files = new File[0];
            }
            Arrays.sort(files, (a, b) -> {
                if (a.isDirectory() != b.isDirectory()) {
                    return a.isDirectory() ? -1 : 1;
                }
                return a.getName().compareToIgnoreCase(b.getName());
            });

            String rel = relativePath(dir);
            StringBuilder body = new StringBuilder();
            body.append("<nav class=\"breadcrumbs\">").append(breadcrumb(rel)).append("</nav>");
            body.append("<header><h1>Phone File Bridge</h1><div class=\"rootPath\">")
                    .append(escape(rootDir.getAbsolutePath())).append("</div></header>");
            if (!message.isEmpty()) {
                body.append("<p class=\"message\">").append(escape(message)).append("</p>");
            }
            body.append(directoryShortcuts());
            body.append("<section class=\"toolbar\">")
                    .append("<label class=\"selectAll\"><input id=\"selectAll\" type=\"checkbox\"> Select all</label>")
                    .append("<button id=\"downloadSelected\" type=\"button\">Download selected</button>")
                    .append("<a class=\"button\" href=\"/?path=").append(enc(parentPath(rel))).append("\">Up</a>")
                    .append("<label class=\"upload\">Upload<input id=\"fileInput\" type=\"file\" multiple></label>")
                    .append("</section>");
            body.append("<section id=\"transferPanel\" class=\"transferPanel\" hidden>")
                    .append("<div class=\"transferTop\"><strong id=\"transferTitle\">Transfer</strong><span id=\"transferPercent\">0%</span></div>")
                    .append("<div class=\"progressTrack\"><div id=\"progressBar\" class=\"progressBar\"></div></div>")
                    .append("<div id=\"transferResult\" class=\"transferResult\"></div>")
                    .append("</section>");
            body.append("<div class=\"path\">/").append(escape(rel)).append("</div>");
            body.append("<table><tbody>");
            appendParentRow(body, rel);
            for (File file : files) {
                String childRel = relativePath(file);
                body.append("<tr><td class=\"name\">");
                if (file.isDirectory()) {
                    body.append("<a href=\"/?path=").append(enc(childRel)).append("\">")
                            .append(escape(file.getName())).append("/</a>");
                } else {
                    body.append("<label class=\"fileRow\"><input class=\"fileCheck\" type=\"checkbox\" value=\"")
                            .append(escape(childRel)).append("\"><span>")
                            .append(escape(file.getName())).append("</span></label>");
                }
                body.append("</td><td>").append(file.isDirectory() ? "Folder" : readableSize(file.length())).append("</td><td>");
                if (file.isFile()) {
                    body.append("<a href=\"/download?path=").append(enc(childRel)).append("\">Download</a>");
                }
                body.append("</td></tr>");
            }
            body.append("</tbody></table>");
            body.append(transferScript(rel));
            sendText(out, 200, "text/html; charset=utf-8", page("Files", body.toString()));
        }

        private String breadcrumb(String rel) throws IOException {
            StringBuilder builder = new StringBuilder();
            builder.append("<a href=\"/?path=\">Storage</a>");
            if (rel == null || rel.isEmpty()) {
                return builder.toString();
            }
            String[] parts = rel.split("/");
            String current = "";
            for (String part : parts) {
                if (part.isEmpty()) {
                    continue;
                }
                current = current.isEmpty() ? part : current + "/" + part;
                builder.append("<span>/</span><a href=\"/?path=")
                        .append(enc(current)).append("\">")
                        .append(escape(part)).append("</a>");
            }
            return builder.toString();
        }

        private void appendParentRow(StringBuilder body, String rel) throws IOException {
            String parent = parentPath(rel);
            boolean hasParent = rel != null && !rel.isEmpty();
            boolean canOpen = hasParent && resolvePath(parent).canRead();
            body.append("<tr class=\"parentRow\"><td class=\"name\">");
            if (canOpen) {
                body.append("<a class=\"parentLink\" href=\"/?path=").append(enc(parent)).append("\">.. Up one level</a>");
            } else {
                body.append("<span class=\"parentDisabled\">.. Up one level</span>");
            }
            body.append("</td><td>").append(canOpen ? "Parent" : "Unavailable").append("</td><td></td></tr>");
        }

        private String directoryShortcuts() throws IOException {
            StringBuilder body = new StringBuilder();
            body.append("<section class=\"shortcuts\">")
                    .append("<span>Quick folders</span>");
            appendShortcut(body, "Download", Environment.DIRECTORY_DOWNLOADS);
            appendShortcut(body, "Documents", Environment.DIRECTORY_DOCUMENTS);
            appendShortcut(body, "Photos", Environment.DIRECTORY_PICTURES);
            appendShortcut(body, "Camera", Environment.DIRECTORY_DCIM);
            appendShortcut(body, "Movies", Environment.DIRECTORY_MOVIES);
            appendShortcut(body, "Music", Environment.DIRECTORY_MUSIC);
            body.append("</section>");
            return body.toString();
        }

        private void appendShortcut(StringBuilder body, String label, String type) throws IOException {
            File dir = Environment.getExternalStoragePublicDirectory(type);
            boolean accessible = dir != null && ((dir.exists() && dir.canRead()) || dir.mkdirs());
            if (accessible) {
                body.append("<a href=\"/?path=").append(enc(relativePath(dir))).append("\">")
                        .append(escape(label)).append("</a>");
            } else {
                body.append("<span class=\"disabledShortcut\">").append(escape(label)).append("</span>");
            }
        }

        private String transferScript(String rel) throws IOException {
            return "<script>"
                    + "var uploadPath='" + enc(rel) + "';"
                    + "var input=document.getElementById('fileInput');"
                    + "var selectAll=document.getElementById('selectAll');"
                    + "var downloadSelected=document.getElementById('downloadSelected');"
                    + "var panel=document.getElementById('transferPanel');"
                    + "var title=document.getElementById('transferTitle');"
                    + "var percent=document.getElementById('transferPercent');"
                    + "var bar=document.getElementById('progressBar');"
                    + "var result=document.getElementById('transferResult');"
                    + "function baseName(path){var p=(path||'file').split('/');return p[p.length-1]||'file';}"
                    + "function showTransfer(text){panel.hidden=false;title.textContent=text;percent.textContent='0%';bar.className='progressBar';bar.style.width='0%';result.className='transferResult';result.textContent='';}"
                    + "function setProgress(loaded,total){if(total>0){var p=Math.max(0,Math.min(100,Math.round(loaded*100/total)));bar.className='progressBar';bar.style.width=p+'%';percent.textContent=p+'%';}else{bar.className='progressBar active';bar.style.width='45%';percent.textContent='Working';}}"
                    + "function finish(ok,text){bar.className='progressBar';bar.style.width=ok?'100%':'0%';percent.textContent=ok?'100%':'Failed';result.className='transferResult '+(ok?'ok':'bad');result.textContent=text;}"
                    + "function saveBlob(blob,name){var url=URL.createObjectURL(blob);var a=document.createElement('a');a.href=url;a.download=name;document.body.appendChild(a);a.click();setTimeout(function(){URL.revokeObjectURL(url);document.body.removeChild(a);},1000);}"
                    + "function nativeDownload(url){var iframe=document.createElement('iframe');iframe.style.display='none';iframe.src=url;document.body.appendChild(iframe);setTimeout(function(){document.body.removeChild(iframe);},60000);}function downloadBlob(method,url,body,name,label){showTransfer(label);var xhr=new XMLHttpRequest();xhr.open(method,url,true);xhr.responseType='blob';if(method==='POST'){xhr.setRequestHeader('Content-Type','application/x-www-form-urlencoded; charset=UTF-8');}xhr.onprogress=function(e){setProgress(e.loaded,e.lengthComputable?e.total:0);};xhr.onload=function(){if(xhr.status>=200&&xhr.status<300){saveBlob(xhr.response,name);finish(true,'Download complete: '+name);}else{finish(false,'Download failed: HTTP '+xhr.status);}};xhr.onerror=function(){finish(false,'Progress download failed; falling back to browser download.');if(method==='GET'){nativeDownload(url);}};xhr.ontimeout=function(){finish(false,'Download timed out; falling back to browser download.');if(method==='GET'){nativeDownload(url);}};xhr.timeout=0;xhr.send(body);}"
                    + "selectAll.onchange=function(){var boxes=document.querySelectorAll('.fileCheck');for(var i=0;i<boxes.length;i++){boxes[i].checked=selectAll.checked;}};"
                    + "downloadSelected.onclick=function(){var boxes=document.querySelectorAll('.fileCheck:checked');var paths=[];for(var i=0;i<boxes.length;i++){paths.push(boxes[i].value);}if(!paths.length){panel.hidden=false;finish(false,'Please select files first.');return;}downloadBlob('POST','/download_zip','paths='+encodeURIComponent(paths.join('\\n')),'selected-files.zip','Packing '+paths.length+' files');};"
                    + "var links=document.querySelectorAll('a[href^=\"/download?path=\"]');for(var i=0;i<links.length;i++){links[i].onclick=function(e){e.preventDefault();var href=this.getAttribute('href');var path=decodeURIComponent((href.split('path=')[1]||'file').replace(/\\+/g,'%20'));var name=baseName(path);downloadBlob('GET',href,null,name,'Downloading '+name);};}"
                    + "function uploadAt(files,index){if(index>=files.length){finish(true,'Upload complete: '+files.length+' file(s)');setTimeout(function(){location.reload();},900);return;}var file=files[index];showTransfer('Uploading '+(index+1)+'/'+files.length+': '+file.name);var xhr=new XMLHttpRequest();xhr.open('POST','/upload_raw?path='+uploadPath+'&name='+encodeURIComponent(file.name),true);xhr.upload.onprogress=function(e){setProgress(e.loaded,e.lengthComputable?e.total:0);};xhr.onload=function(){if(xhr.status>=200&&xhr.status<300){uploadAt(files,index+1);}else{finish(false,'Upload failed: '+file.name+' HTTP '+xhr.status);}};xhr.onerror=function(){finish(false,'Upload failed: '+file.name+' network error');};xhr.send(file);}"
                    + "input.onchange=function(){if(input.files&&input.files.length){uploadAt(input.files,0);}};"
                    + "</script>";
        }

        private void sendDownload(OutputStream out, String requestedPath) throws IOException {
            File file = resolvePath(requestedPath);
            if (!file.exists() || !file.isFile()) {
                sendText(out, 404, "text/plain; charset=utf-8", "File not found");
                return;
            }
            String headers = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: application/octet-stream\r\n"
                    + "Content-Length: " + file.length() + "\r\n"
                    + "Content-Disposition: attachment; filename=\"" + asciiFileName(file.getName()) + "\"; filename*=UTF-8''" + enc(file.getName()) + "\r\n"
                    + "Cache-Control: no-store\r\n"
                    + "Connection: close\r\n\r\n";
            out.write(headers.getBytes(StandardCharsets.UTF_8));
            FileInputStream fileIn = new FileInputStream(file);
            try {
                copy(fileIn, out, file.length());
                out.flush();
            } finally {
                closeQuietly(fileIn);
            }
        }

        private void sendZipDownload(OutputStream out, String requestedPaths) throws IOException {
            if (requestedPaths == null || requestedPaths.trim().isEmpty()) {
                sendText(out, 400, "text/plain; charset=utf-8", "No files selected");
                return;
            }

            String[] paths = requestedPaths.split("\\n");
            List<File> selected = new ArrayList<>();
            for (String path : paths) {
                if (path.trim().isEmpty()) {
                    continue;
                }
                File file = resolvePath(path);
                if (file.exists() && file.isFile()) {
                    selected.add(file);
                }
            }
            if (selected.isEmpty()) {
                sendText(out, 404, "text/plain; charset=utf-8", "Selected files were not found");
                return;
            }

            String headers = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: application/zip\r\n"
                    + "Content-Disposition: attachment; filename=\"selected-files.zip\"\r\n"
                    + "Connection: close\r\n\r\n";
            out.write(headers.getBytes(StandardCharsets.UTF_8));

            ZipOutputStream zipOut = new ZipOutputStream(out);
            byte[] buffer = new byte[64 * 1024];
            for (File file : selected) {
                String entryName = relativePath(file);
                zipOut.putNextEntry(new ZipEntry(entryName));
                FileInputStream fileIn = new FileInputStream(file);
                try {
                    int read;
                    while ((read = fileIn.read(buffer)) != -1) {
                        zipOut.write(buffer, 0, read);
                    }
                } finally {
                    closeQuietly(fileIn);
                    zipOut.closeEntry();
                }
            }
            zipOut.finish();
        }

        private void handleUpload(Request request, InputStream in, OutputStream out) throws IOException {
            File dir = resolvePath(request.query.get("path"));
            if (!dir.exists() || !dir.isDirectory()) {
                sendText(out, 404, "text/plain; charset=utf-8", "Upload directory not found");
                return;
            }
            String rawName = request.query.get("name");
            if (rawName == null || rawName.trim().isEmpty()) {
                sendText(out, 400, "text/plain; charset=utf-8", "Missing file name");
                return;
            }
            File target = new File(dir, new File(rawName).getName());
            FileOutputStream fileOut = new FileOutputStream(target);
            copy(in, fileOut, request.contentLength);
            closeQuietly(fileOut);
            sendText(out, 200, "text/plain; charset=utf-8", "OK");
        }

        private File resolvePath(String relative) throws IOException {
            String target = relative == null ? defaultPath : relative;
            File file = target.isEmpty() ? rootDir : new File(rootDir, target);
            String rootPath = rootDir.getCanonicalPath();
            String filePath = file.getCanonicalPath();
            if (!filePath.equals(rootPath) && !filePath.startsWith(rootPath + File.separator)) {
                throw new IOException("Invalid path");
            }
            return file;
        }

        private String relativePath(File file) throws IOException {
            String rootPath = rootDir.getCanonicalPath();
            String filePath = file.getCanonicalPath();
            if (filePath.equals(rootPath)) {
                return "";
            }
            return filePath.substring(rootPath.length() + 1).replace(File.separatorChar, '/');
        }

        private String relativePathQuietly(File file, String fallback) {
            try {
                return relativePath(file);
            } catch (IOException e) {
                return fallback;
            }
        }

        private String parentPath(String rel) {
            if (rel == null || rel.isEmpty()) {
                return "";
            }
            int slash = rel.lastIndexOf('/');
            return slash > 0 ? rel.substring(0, slash) : "";
        }

        private File getSharedDownloadDirectory() {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (dir != null) {
                return dir;
            }
            return new File(Environment.getExternalStorageDirectory(), "Download");
        }

        private Map<String, String> parseQuery(String query) throws IOException {
            Map<String, String> result = new HashMap<>();
            if (query == null || query.isEmpty()) {
                return result;
            }
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int eq = pair.indexOf('=');
                String key = eq >= 0 ? pair.substring(0, eq) : pair;
                String value = eq >= 0 ? pair.substring(eq + 1) : "";
                result.put(URLDecoder.decode(key, "UTF-8"), URLDecoder.decode(value, "UTF-8"));
            }
            return result;
        }

        private void sendRedirect(OutputStream out, String location, String extraHeaders) throws IOException {
            String response = "HTTP/1.1 302 Found\r\n"
                    + "Location: " + location + "\r\n"
                    + extraHeaders
                    + "Content-Length: 0\r\n"
                    + "Connection: close\r\n\r\n";
            out.write(response.getBytes(StandardCharsets.UTF_8));
        }

        private void sendText(OutputStream out, int code, String contentType, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            String reason = code == 200 ? "OK" : code == 302 ? "Found" : code == 404 ? "Not Found" : "Error";
            String headers = "HTTP/1.1 " + code + " " + reason + "\r\n"
                    + "Content-Type: " + contentType + "\r\n"
                    + "Content-Length: " + bytes.length + "\r\n"
                    + "Connection: close\r\n\r\n";
            out.write(headers.getBytes(StandardCharsets.UTF_8));
            out.write(bytes);
        }

        private String page(String title, String body) {
            return "<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\">"
                    + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                    + "<title>" + escape(title) + "</title><style>"
                    + ":root{color-scheme:light}*{box-sizing:border-box}body{font-family:system-ui,-apple-system,Segoe UI,sans-serif;margin:0;background:#eef4f8;color:#162235}"
                    + ".breadcrumbs{position:sticky;top:0;z-index:3;display:flex;gap:8px;align-items:center;flex-wrap:wrap;background:#0b1b2b;color:#b8c7d6;padding:10px 22px;border-bottom:1px solid rgba(255,255,255,.12)}"
                    + ".breadcrumbs a{color:#ffffff;text-decoration:none;font-weight:650}.breadcrumbs span{color:#4ecdc4}"
                    + "header{background:#102033;color:#fff;padding:22px 28px;border-bottom:4px solid #4ecdc4}"
                    + "header div{color:#b8c7d6;font-family:ui-monospace,monospace;word-break:break-all}h1{font-size:24px;margin:0 0 6px;letter-spacing:0}"
                    + ".shortcuts{display:flex;gap:8px;align-items:center;flex-wrap:wrap;padding:12px 22px;background:#fff;border-bottom:1px solid #d8e3ed}.shortcuts span:first-child{color:#4a5c70;font-weight:650;margin-right:4px}"
                    + ".shortcuts a,.disabledShortcut{border:1px solid #c7d5e2;border-radius:999px;padding:7px 12px;text-decoration:none;font-weight:650}.shortcuts a{color:#1459b8;background:#f7fbfd}.shortcuts a:hover{background:#eaf4fb}.disabledShortcut{color:#94a3b8;background:#f1f5f9}"
                    + ".toolbar{position:sticky;top:42px;z-index:2;display:flex;gap:10px;align-items:center;padding:14px 22px;flex-wrap:wrap;background:rgba(238,244,248,.96);border-bottom:1px solid #d8e3ed}"
                    + ".selectAll{display:flex;align-items:center;gap:8px;color:#27384d;font-weight:650}.selectAll input{width:18px;height:18px;accent-color:#1459b8}"
                    + ".button,.upload,button{background:#1459b8;color:#fff;border:0;border-radius:7px;padding:10px 14px;text-decoration:none;cursor:pointer;font-weight:650}"
                    + ".button:hover,.upload:hover,button:hover{background:#0e4d9f}.upload{display:inline-block}.upload input{display:none}"
                    + ".path{font-family:ui-monospace,monospace;margin:16px 22px;padding:12px 14px;background:#fff;border:1px solid #d6e2ed;border-radius:8px;color:#4a5c70;word-break:break-all}"
                    + ".transferPanel{margin:16px 22px 0;padding:14px;background:#fff;border:1px solid #d6e2ed;border-radius:8px;box-shadow:0 8px 24px rgba(16,32,51,.06)}"
                    + ".transferTop{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:10px}.transferTop span{color:#4a5c70;font-family:ui-monospace,monospace}"
                    + ".progressTrack{height:10px;background:#dbe7f0;border-radius:999px;overflow:hidden}.progressBar{height:100%;width:0;background:#4ecdc4;border-radius:999px;transition:width .18s ease}.progressBar.active{animation:pulse 1.1s infinite alternate}"
                    + ".transferResult{margin-top:10px;color:#4a5c70}.transferResult.ok{color:#0f7a55}.transferResult.bad{color:#b91c1c}@keyframes pulse{from{transform:translateX(-20%)}to{transform:translateX(145%)}}"
                    + ".fileRow{display:flex;align-items:center;gap:12px}.fileCheck{width:18px;height:18px;accent-color:#1459b8}.checkSpace{display:inline-block;width:30px}"
                    + ".parentRow{background:#f7fbfd}.parentLink{display:inline-block}.parentDisabled{color:#94a3b8;font-weight:650}"
                    + "table{width:calc(100% - 44px);margin:0 22px 24px;border-collapse:separate;border-spacing:0;background:#fff;border:1px solid #d6e2ed;border-radius:8px;overflow:hidden}"
                    + "td{border-top:1px solid #e4edf4;padding:13px 16px}tr:first-child td{border-top:0}tr:hover{background:#f7fbfd}td.name{width:70%;word-break:break-word}"
                    + "a{color:#1459b8;font-weight:650}.login{max-width:380px;margin:12vh auto;padding:26px;background:#fff;border:1px solid #d6e2ed;border-radius:8px;box-shadow:0 18px 55px rgba(16,32,51,.12)}"
                    + ".login input{box-sizing:border-box;width:100%;padding:12px;margin:12px 0;border:1px solid #c7d5e2;border-radius:7px}.login button{width:100%;font-size:16px}.error,.message{margin:14px 22px;color:#b91c1c}"
                    + "@media(max-width:640px){header{padding:18px}.breadcrumbs,.shortcuts{padding:10px 16px}.toolbar{top:40px;padding:12px 16px}.path,.transferPanel,table{margin-left:16px;margin-right:16px;width:calc(100% - 32px)}td{display:block;border-top:0;padding:6px 14px}tr{display:block;border-top:1px solid #e4edf4;padding:8px 0}}"
                    + "</style></head><body>" + body + "</body></html>";
        }

        private String newToken() {
            byte[] bytes = new byte[24];
            random.nextBytes(bytes);
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format(Locale.US, "%02x", b));
            }
            return builder.toString();
        }

        private byte[] readBytes(InputStream in, long count) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            copy(in, out, count);
            return out.toByteArray();
        }

        private void copy(InputStream in, OutputStream out, long count) throws IOException {
            byte[] buffer = new byte[64 * 1024];
            long remaining = count;
            while (remaining > 0) {
                int read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) {
                    break;
                }
                out.write(buffer, 0, read);
                remaining -= read;
            }
        }

        private long parseLong(String value, long fallback) {
            try {
                return value == null ? fallback : Long.parseLong(value);
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        private String readableSize(long size) {
            String[] units = {"B", "KB", "MB", "GB"};
            double value = size;
            int unit = 0;
            while (value >= 1024 && unit < units.length - 1) {
                value /= 1024;
                unit++;
            }
            return String.format(Locale.US, unit == 0 ? "%.0f %s" : "%.1f %s", value, units[unit]);
        }

        private String enc(String value) throws IOException {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8").replace("+", "%20");
        }

        private String asciiFileName(String value) {
            if (value == null || value.isEmpty()) {
                return "download";
            }
            String cleaned = value.replace("\\", "_").replace("/", "_").replace("\"", "_");
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < cleaned.length(); i++) {
                char ch = cleaned.charAt(i);
                builder.append(ch >= 32 && ch < 127 ? ch : '_');
            }
            return builder.length() == 0 ? "download" : builder.toString();
        }

        private String js(String value) {
            return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'");
        }

        private String escape(String value) {
            if (value == null) {
                return "";
            }
            return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
        }

        private void closeQuietly(Closeable closeable) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static class Request {
        String method;
        String path;
        Map<String, String> query = Collections.emptyMap();
        Map<String, String> headers = Collections.emptyMap();
        long contentLength;
    }
}
