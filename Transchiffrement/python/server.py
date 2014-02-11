import socket
from SocketServer import BaseServer
from BaseHTTPServer import HTTPServer
from SimpleHTTPServer import SimpleHTTPRequestHandler
from OpenSSL import SSL
from BaseHTTPServer import BaseHTTPRequestHandler


class HTTPSServer(HTTPServer):
    def __init__(self, server_address, HandlerClass):
        BaseServer.__init__(self, server_address, HandlerClass)
        ctx = SSL.Context(SSL.SSLv23_METHOD)
        fpem = '/home/emile/certificat_serveur/server.pem'
        cert = '/home/emile/certificat_serveur/server.crt'
        ctx.use_privatekey_file (fpem)
        ctx.use_certificate_file(cert)
        self.socket = SSL.Connection(ctx, socket.socket(self.address_family, self.socket_type))
        self.server_bind()
        self.server_activate()


class myHandler(SimpleHTTPRequestHandler):
    def setup(self):
        self.connection = self.request
        self.rfile = socket._fileobject(self.request, "rb", self.rbufsize)
        self.wfile = socket._fileobject(self.request, "wb", self.wbufsize)
        
	def shutdown_request(self,request):
		request.shutdown()
	
	def shutdown(self, args=None):
         return self._con.shutdown()



server_address = ('127.0.0.1', 8080)
httpd = HTTPSServer(server_address, myHandler)
sa = httpd.socket.getsockname()
print "Serving HTTPS on", sa[0], "port", sa[1], "..."
httpd.serve_forever()
