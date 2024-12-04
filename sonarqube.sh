mvn clean verify sonar:sonar \
  -Dsonar.projectKey=blas-opentelemetry-agent-extension \
  -Dsonar.projectName='blas-opentelemetry-agent-extension' \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=sqp_0b0f140f3ef82d51d1ecb6c57a4221e5f033a30e
