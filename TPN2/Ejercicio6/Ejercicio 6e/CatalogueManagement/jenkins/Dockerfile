FROM jenkins/jenkins:lts-jdk17
USER root
COPY --from=docker:cli /usr/local/bin/docker /usr/local/bin/docker
RUN jenkins-plugin-cli --plugins workflow-aggregator git maven-plugin docker-workflow junit pipeline-stage-view
