import socket, ssl, pprint

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
ssl_sock = ssl.wrap_socket(s, ca_certs="/home/emile/Desktop/Certificats/cert.pem", cert_reqs=ssl.CERT_REQUIRED)
ssl_sock.connect(('192.168.56.101', 443))

pprint.pprint(ssl_sock.getpeercert())
ssl_sock.send('GET /\n')
print ssl_sock.recv(4096)
ssl_sock.close()
