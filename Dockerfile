FROM ubuntu:16.04

# Обвновление списка пакетов
RUN apt-get -y update

#
# Установка postgresql
#
ENV PGVER 9.5
RUN apt-get install -y postgresql-$PGVER

# Run the rest of the commands as the ``postgres`` user created by the ``postgres-$PGVER`` package when it was ``apt-get installed``
USER postgres

# Create a PostgreSQL role named ``docker`` with ``docker`` as the password and
# then create a database `docker` owned by the ``docker`` role.
RUN /etc/init.d/postgresql start &&\
    psql --command "CREATE USER docker WITH SUPERUSER PASSWORD 'docker';" &&\
    createdb -O docker docker &&\
    /etc/init.d/postgresql stop

# Adjust PostgreSQL configuration so that remote connections to the
# database are possible.
RUN echo "host all  all    0.0.0.0/0  md5" >> /etc/postgresql/$PGVER/main/pg_hba.conf

# And add ``listen_addresses`` to ``/etc/postgresql/$PGVER/main/postgresql.conf``
RUN echo "listen_addresses='*'" >> /etc/postgresql/$PGVER/main/postgresql.conf

# Expose the PostgreSQL port
EXPOSE 5432

# Add VOLUMEs to allow backup of config, logs and databases
VOLUME  ["/etc/postgresql", "/var/log/postgresql", "/var/lib/postgresql"]

# Back to the root user
USER root

#
# Сборка проекта
#

# Установка JDK
RUN apt-get install -y openjdk-8-jdk-headless

RUN apt-get install -y maven

# Копируем исходный код в Docker-контейнер
ENV WORK /opt/tech-db-forum
ADD src/ $WORK/src/
ADD pom.xml $WORK/



WORKDIR $WORK/
RUN apt-get install -y cron
ADD tab /etc/cron.d/vacuum-cron
RUN chmod 0644 /etc/cron.d/vacuum-cron
RUN touch /var/log/cron.log
CMD cron && tail -f /var/log/cron.log

# Собираем и устанавливаем пакет
RUN mvn compile

# Объявлем порт сервера
EXPOSE 5000

#
# Запускаем PostgreSQL и сервер
#
CMD service postgresql start && mvn exec:java
