# Backend API VPS Logs Follow backend container logs live: 

```bash ssh -i /Users/danielfalconruiz/.ssh/id_ed25519 root@76.13.56.38 \
  'docker logs -f --tail 200 eatsmart-backend' ``` 

Show last 200 lines and exit: 

```bash ssh -i /Users/danielfalconruiz/.ssh/id_ed25519 
root@76.13.56.38 \
  'docker logs --tail 200 eatsmart-backend' ``` 

Show logs from the last hour: 

```bash ssh -i /Users/danielfalconruiz/.ssh/id_ed25519 root@76.13.56.38 
\
  'docker logs --since 1h eatsmart-backend'
```
