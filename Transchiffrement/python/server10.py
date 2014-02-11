# -*- coding: utf-8 -*-
import BaseHTTPServer

#~ ================================
#~   /!\ HTTP /!\
#~ ================================

HOST_NAME = '127.0.0.1'
PORT_NUMBER = 8080


class MyHandler(BaseHTTPServer.BaseHTTPRequestHandler):
    def do_HEAD(s):
        s.send_response(200)
        s.send_header("Content-type", "text/html")
        s.end_headers()
        
    def do_GET(s):
        """Respond to a GET request."""
        s.send_response(200)
        s.send_header("Content-type", "text/html")
        s.end_headers()
        s.wfile.write("<html><head><title>Transchiffrement</title></head>")
        s.wfile.write("<body><p>C'est bon ca marche !</p>")
        s.wfile.write("<p>Chemin demande : %s</p>" % s.path)
        s.wfile.write("</body></html>")

if __name__ == '__main__':
    httpd = BaseHTTPServer.HTTPServer((HOST_NAME, PORT_NUMBER), MyHandler)
    print "%s:%s" % (HOST_NAME, PORT_NUMBER)
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print 'Ctrl-C'
    httpd.server_close()
    print 'arrêt du serveur '
