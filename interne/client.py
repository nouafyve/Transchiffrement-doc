from OpenSSL import crypto
from M2Crypto import X509
import os, socket, ssl, pprint

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
ssl_sock = ssl.wrap_socket(s, ca_certs="/home/pyrow/Transchiffrement-doc/interne/cles/cert.pem", cert_reqs=ssl.CERT_REQUIRED)
ssl_sock.connect(('10.0.0.10', 443))
cert_string = ssl.get_server_certificate(('10.0.0.10',443))
x509 = X509.load_cert_string(cert_string, X509.FORMAT_PEM)
x509.save_pem("cles/test.pem")
os.system("java -cp bcprov-jdk12-130.jar:. GenerateCertificate")
ssl_sock.send('GET /\n')
print ssl_sock.recv(4096)
ssl_sock.close()