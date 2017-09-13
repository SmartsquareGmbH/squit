Insert into CBD_TAX_RATE_T (SEQ_NO_TR,GA_CODE,GA_TYPE,TAX_CODE,TAX_RATE,VALID_FROM,VALID_TO,IDRS_IMPORT_DATE,DELETE_DATE) values (5000001,'DE','CTY','2',7,to_date('27-OCT-04','DD-MON-RR'),null,to_date('11-FEB-15','DD-MON-RR'),null);
update cbd_tax_rate_t set valid_to=to_date('19-DEC-13','DD-MON-RR') where seq_no_tr='474';
Insert into CBD_TAX_RATE_T (SEQ_NO_TR,GA_CODE,GA_TYPE,TAX_CODE,TAX_RATE,VALID_FROM,VALID_TO,IDRS_IMPORT_DATE,DELETE_DATE) values (5000002,'DE','CTY','0',2,to_date('20-DEC-13','DD-MON-RR'),null,to_date('04-APR-13','DD-MON-RR'),null);
update cbd_ga_taxtypes_t set valid_to=to_date('19-DEC-13','DD-MON-RR') where seq_no_gtt in (2880,2710,2711);
Insert into CBD_GA_TAXTYPES_T (SEQ_NO_GTT,SEQ_NO_GA,TAX_TYPE,TAX_TYPE_DESCR,TAX_CODE,VALID_FROM,VALID_TO,IDRS_IMPORT_DATE,DELETE_DATE) values (5000001,48533,'GST','TEST','0',to_date('20-DEC-13','DD-MON-RR'),null,to_date('04-APR-13','DD-MON-RR'),null);
