from http.server import BaseHTTPRequestHandler, HTTPServer

class SimpleHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header('Content-type', 'text/plain; charset=utf-8')
        self.end_headers()
        self.wfile.write("Backend is Working! (Port 8081)\nDB Connection Info: Check Console Logs".encode())

# 8080 포트에서 실행 (도커 내부 포트)
server_address = ('', 8080)
httpd = HTTPServer(server_address, SimpleHandler)
print("Starting Test Server on port 8080...")
httpd.serve_forever()