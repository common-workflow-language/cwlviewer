--liquibase formatted sql

--changeset kinow:create-workflow-table
create table if not exists workflow
(
    id                varchar(36) not null
        primary key,
    cwltool_version   text,
    doc               text,
    docker_link       text,
    inputs            jsonb,
    label             text,
    last_commit       text,
    license_link      text,
    outputs           jsonb,
    retrieved_from    jsonb
        constraint unique_workflow_retrieved_from
            unique,
    retrieved_on      timestamp,
    ro_bundle_path    text,
    steps             jsonb,
    visualisation_dot text
);
--rollback drop table workflow;

--changeset kinow:create-idx_workflow_retrieved_on-index
create index if not exists idx_workflow_retrieved_on
    on workflow (retrieved_on);
--rollback drop index idx_workflow_retrieved_on;

--changeset kinow:create-queued_workflow-table
create table if not exists queued_workflow
(
    id                  varchar(36) not null
        primary key,
    cwltool_status      jsonb,
    cwltool_version     text,
    message             text,
    temp_representation jsonb,
    workflow_list       jsonb
);
--rollback drop table queued_workflow;
