/*! SET storage_engine=INNODB */;

drop table if exists accertify_responses;
create table accertify_responses (
  record_id int(11) unsigned not null auto_increment
, kb_account_id char(36) not null
, kb_payment_external_key char(128) not null
, kb_payment_transaction_external_key char(128) not null
, transaction_type varchar(32) not null
, amount numeric(15,9)
, currency char(3)
, transaction_id varchar(64)
, cross_reference varchar(64)
, rules_tripped varchar(1024)
, total_score varchar(64)
, recommendation_code varchar(64)
, remarks varchar(1024)
, additional_data longtext
, created_date datetime not null
, kb_tenant_id char(36) not null
, primary key(record_id)
) /*! CHARACTER SET utf8 COLLATE utf8_bin */;
create index accertify_responses_transaction_id on accertify_responses(transaction_id);
create index accertify_responses_kb_payment_external_key on accertify_responses(kb_payment_external_key);
create index accertify_responses_kb_payment_transaction_external_key on accertify_responses(kb_payment_transaction_external_key);
