Insert into PA_GA_EXCEPTION_T (SEQ_NO_GAX,SEQ_NO_PARAM,CTY_CODE,STA_CODE,DESCR,STA_MAND_FLAG,VALID_FROM,VALID_TO,USERID,DELETE_DATE) values (5000000,17,'CH','*',null,'N',to_date('01-JUN-12','DD-MON-RR'),to_date('21-MAR-15','DD-MON-RR'),'tim.user1',null);
Insert into PA_GA_EXCEPTION_T (SEQ_NO_GAX,SEQ_NO_PARAM,CTY_CODE,STA_CODE,DESCR,STA_MAND_FLAG,VALID_FROM,VALID_TO,USERID,DELETE_DATE) values (5000001,17,'DE','*',null,'N',to_date('01-JUN-12','DD-MON-RR'),to_date('21-MAR-15','DD-MON-RR'),'tim.user1',null);
update cbd_bu_main_address_t set sta_code=null where bu_code='119' and bu_type='STO' and VALID_FROM=to_date('18-JUL-12','DD-MON-RR');
update cbd_bu_main_address_t set sta_code=null where bu_code='1206' and bu_type='COM' and VALID_FROM=to_date('03-APR-13','DD-MON-RR');
