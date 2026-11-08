
```yaml
services:
  nexus:
    image: sonatype/nexus3:<pin-a-version>   # pick a tag from Docker Hub, avoid :latest
    container_name: nexus
    restart: unless-stopped
    ports:
      - "8091:8081"
      - "5000:5000"        
    environment:
      - INSTALL4J_ADD_VM_PARAMS=-Xms1g -Xmx2g -XX:MaxDirectMemorySize=2g
    volumes:
      - nexus-data:/nexus-data

volumes:
  nexus-data:
```

- **Non-root:** the official image already runs as the `nexus` user (uid 200), so you don't need to set `user:`. A named volume avoids permission problems. If you use a bind mount instead, run `sudo chown -R 200:200 ./nexus-data`.
- **Java:** the image bundles its own JRE, so the Java 11 requirement only applies to your Maven builds, not to the Nexus container.
- **Memory:** adjust the heap values to what your Oracle instance has free. Nexus is heavy and starts slowly.

**2. Start it and wait**

```bash
docker compose up -d nexus
docker logs -f nexus     # wait for "Started Sonatype Nexus"
```

**3. Get the admin password**

```bash
docker exec nexus cat /nexus-data/admin.password
```

Open `http://<server-ip>:8091`, sign in as `admin` with that password, and follow the wizard to set a new password. Disable anonymous access when it asks.

**4. If Jenkins runs in the same compose file**

Jenkins can reach Nexus by service name at `http://nexus:8081` (the internal port stays 8081). Use that URL in `settings.xml` on the Jenkins side.

**5. creating the repositories**

You can do this in the Nexus UI: ⚙️ **Settings → Repositories → Create repository**.

**1. Hosted releases**
- Recipe: `maven2 (hosted)`
- Name: `buy02-releases`
- Version policy: **Release**
- Deployment policy: **Disable redeploy** (releases stay immutable)

**2. Hosted snapshots**
- Recipe: `maven2 (hosted)`
- Name: `buy02-snapshots`
- Version policy: **Snapshot**
- Deployment policy: **Allow redeploy**

**3. Proxy to Maven Central**
- Recipe: `maven2 (proxy)`
- Name: `maven-central-proxy`
- Remote storage: `https://repo1.maven.org/maven2/`
- Version policy: Release
- Leave the other defaults.

**4. Group**
- Recipe: `maven2 (group)`
- Name: `buy02-group`
- Members, in this order: `buy02-releases`, `buy02-snapshots`, `maven-central-proxy`, `maven-central(proxy)`

**5. Deployer role and user**
- **Settings → Security → Roles → Create role** (Nexus role):
  - ID: `buy02-deployer`
  - Privileges: `nx-repository-view-maven2-buy02-releases-*`, `nx-repository-view-maven2-buy02-snapshots-*`, `nx-repository-view-maven2-buy02-group-read`
- **Settings → Security → Users → Create local user**:
  - ID: `jenkins-deployer`
  - Strong password, role `buy02-deployer`

**6. Quick check**

```bash
curl -I -u 'jenkins-deployer:<password>' http://localhost:8091/repository/buy02-group/
```
```bash
curl -I -u "jenkins-deployer:$NEXUS_PASSWORD" \
  http://localhost:8091/repository/maven-central-proxy/commons-jxpath/commons-jxpath/1.3/commons-jxpath-1.3.jar
```
```bash
curl -I -u "jenkins-deployer:$NEXUS_PASSWORD" \
  http://localhost:8091/repository/buy02-group/org/springframework/boot/spring-boot-buildpack-platform/3.4.12/spring-boot-buildpack-platform-3.4.12.jar
```

You should get `200` or `401`. A `404` means the group name is wrong.

**7. Configure the Pushing Machine (daemon.json)**

By default, Docker expects registries to use TLS/HTTPS. Because you are omitting TLS, the client machine pushing to Nexus will fail with a server gave HTTP response to HTTPS client error unless it's configured as insecure. [1] (https://www.stephane-robert.info/docs/developper/artefacts/nexus/registry-docker/)
Depending on the operating system of the machine doing the docker push, use one of the methods below: [1] (https://nickjanetakis.com/blog/docker-tip-50-running-an-insecure-docker-registry)

**8. Create or edit the configuration file at /etc/docker/daemon.json:bash (linux)**
```bash
sudo nano /etc/docker/daemon.json
```
Use code with caution.
**9. Add your Nexus server's IP address or domain name along with port 5000:json**
```json
{
  "insecure-registries": ["<NEXUS_SERVER_IP>:5000"]
}
```
Use code with caution.
**10. Restart the Docker daemon to apply the** 
```bash
sudo systemctl restart docker
```
Use code with caution.

**11. Log In, Tag, and Push**
```bash
# 1. Log in to the Nexus repository (Use your Nexus UI admin credentials)
docker login <NEXUS_SERVER_IP>:5000

# 2. Tag your target image using the Nexus domain prefix
docker tag my-local-image:latest <NEXUS_SERVER_IP>:5000/my-local-image:latest

# 3. Push to your repository
docker push <NEXUS_SERVER_IP>:5000/my-local-image:latest
```

Once the configuration is active, you can interact with your insecure Nexus registry: [1]

bash
# 1. Log in to the Nexus repository (Use your Nexus UI admin credentials)
docker login <NEXUS_SERVER_IP>:5000

# 2. Tag your target image using the Nexus domain prefix
docker tag my-local-image:latest <NEXUS_SERVER_IP>:5000/my-local-image:latest

# 3. Push to your repository
docker push <NEXUS_SERVER_IP>:5000/my-local-image:latest
Use code with caution.
If you hit any authentication snags, let me know:
• Have you already enabled the Docker Bearer Token Realm inside the Nexus Security UI?
• Are you receiving a 401 Unauthorized error or a connection error?


# Troubleshooting (buy-02)

Problems hit while connecting Maven to Nexus (Docker Compose, Nexus 3.x), with the cause and fix for each.

## Quick reference

| # | Symptom | Cause | Fix |
|---|---------|-------|-----|
| 1 | `401 Unauthorized` on `curl` to the group | Anonymous access is disabled | Expected. Test with `-u user:password` |
| 2 | `Malformed POM: Unrecognised tag: 'repository'` | Repos placed inside `<dependencyManagement>` without `<distributionManagement>` | Move them into `<distributionManagement>` under `<project>` |
| 3 | `authentication failed ... status: 401` on deploy | Maven not sending credentials | Create or fix `~/.m2/settings.xml`, match server ids |
| 4 | `Could not find artifact ... in nexus-group` | Group not reaching the proxy, or cached "not found" | Add proxy to the group, invalidate caches, `-U` |
| 5 | Spring Boot plugin artifacts not resolved | Same cached failure as #4 | Same fix as #4 |

---

## 1. `401` when testing the group anonymously

```bash
curl -I http://localhost:8091/repository/buy02-group/
# HTTP/1.1 401 Unauthorized
```

**Cause:** anonymous access is disabled (on purpose).
**Fix:** nothing to fix. Test with credentials:

```bash
curl -I -u "jenkins-deployer:$NEXUS_PASSWORD" http://localhost:8091/repository/buy02-group/
# HTTP/1.1 200 OK
```

## 2. Malformed POM

```
Malformed POM ... Unrecognised tag: 'repository'
```

**Cause:** `<repository>` and `<snapshotRepository>` were inside `<dependencyManagement>` and had no `<distributionManagement>` wrapper.

**Fix:** put them in `<distributionManagement>`, a direct child of `<project>` and a sibling of `<dependencyManagement>` and `<build>`:

```xml
<distributionManagement>
    <repository>
        <id>nexus-releases</id>
        <url>${env.NEXUS_URL}/repository/buy02-releases/</url>
    </repository>
    <snapshotRepository>
        <id>nexus-snapshots</id>
        <url>${env.NEXUS_URL}/repository/buy02-snapshots/</url>
    </snapshotRepository>
</distributionManagement>
```

Export the variables before building:

```bash
export NEXUS_URL=http://localhost:8091
export NEXUS_PASSWORD='<password>'   # single quotes, especially if it has spaces
```

## 3. `authentication failed ... 401` on `mvn deploy`

**Diagnose in two steps:**

1. Test the credentials outside Maven:
   ```bash
   curl -I -u "jenkins-deployer:$NEXUS_PASSWORD" http://localhost:8091/repository/buy02-group/
   ```
   - `401`: wrong password, or user inactive. Check **Settings → Security → Users** and reset it.
   - `200`: credentials are fine, so Maven isn't reading them (next step).
2. See what Maven loads:
   ```bash
   mvn help:effective-settings | head -40
   ```
   If there are no `nexus` entries, `~/.m2/settings.xml` is missing or wrong.

**Fix:** create `~/.m2/settings.xml`. The `<id>` values must match the pom exactly:

```xml
<settings>
  <servers>
    <server>
      <id>nexus-releases</id>
      <username>jenkins-deployer</username>
      <password>${env.NEXUS_PASSWORD}</password>
    </server>
    <server>
      <id>nexus-snapshots</id>
      <username>jenkins-deployer</username>
      <password>${env.NEXUS_PASSWORD}</password>
    </server>
  </servers>
</settings>
```

Use an environment variable (or Jenkins credentials) for the password. Never commit it, and never paste it in chats or tickets. If it was exposed, rotate it.

## 4. `Could not find artifact ... in nexus-group`

Example: `commons-jxpath:commons-jxpath:jar:1.3`.

**Diagnose:** compare the proxy and the group.

```bash
# Proxy
curl -I -u "jenkins-deployer:$NEXUS_PASSWORD" \
  http://localhost:8091/repository/maven-central-proxy/commons-jxpath/commons-jxpath/1.3/commons-jxpath-1.3.jar
# Group
curl -I -u "jenkins-deployer:$NEXUS_PASSWORD" \
  http://localhost:8091/repository/buy02-group/commons-jxpath/commons-jxpath/1.3/commons-jxpath-1.3.jar
```

Proxy `200` but group `404` means the group is not reaching the proxy.

**Fix:**

1. **Settings → Repositories → `buy02-group`**: make sure `maven-central-proxy` is in the **Members** list (right side), then **Save**. Recommended order: `buy02-releases`, `buy02-snapshots`, `maven-central-proxy`.
2. Click **Invalidate cache** on `maven-central-proxy` and on `buy02-group`.
3. Clear Maven's cached failures and force an update:
   ```bash
   find ~/.m2/repository -name "*.lastUpdated" -delete
   find ~/.m2/repository -name "resolver-status.properties" -delete
   mvn clean deploy -DskipTests -U
   ```

Other causes to check:

- Proxy remote URL must be exactly `https://repo1.maven.org/maven2/` and the repo must be **Online**.
- The deployer role needs read access: `nx-repository-view-maven2-buy02-group-read` and the proxy read privilege.
- Outbound access from the container:
  ```bash
  docker exec nexus curl -sI https://repo1.maven.org/maven2/ | head -1
  ```

## 5. Spring Boot plugin dependencies not resolved

```
Plugin org.springframework.boot:spring-boot-maven-plugin:3.4.12 or one of its dependencies could not be resolved
```

**Cause:** the same cached "not found" as #4. These artifacts were requested while the group had no proxy member.

**Fix:** invalidate the Nexus caches, delete the `.lastUpdated` markers, rebuild with `-U` (see #4). The first successful build is slow because Nexus downloads and caches everything from Maven Central. Later builds are much faster.

---

## Notes

- **Java version:** Spring Boot 3.4 requires Java 17+. The "Java 11" requirement cannot apply to this build, so check `java -version` and `mvn -v`, and the Jenkins agent too. The Nexus container uses its own bundled JRE.
- **Ports:** Nexus is mapped `8091:8081` because 8081 is used by another service. Containers on the same compose network (such as Jenkins) use `http://nexus:8081`, so set `NEXUS_URL` accordingly in the pipeline.
- **Security:** do not expose port 8091 publicly. Use an SSH tunnel or an HTTPS reverse proxy.

## Useful checks

```bash
docker logs --tail 50 nexus                  # Nexus errors
mvn help:effective-settings                  # settings Maven actually loads
mvn -v                                       # Maven home and Java version
mvn clean deploy -DskipTests -U              # force update of cached failures
```

