insert into IC_UNSPSC_T (SEQ_NO_UNSP,SEQ_NO_PARENT, UNSPSC_CODE, UNSPSC_BF, LEVEL_NUMBER, TITLE, DESCRIPTION, VALID_FROM, VALID_TO,userid, DELETE_DATE) values (5000009,null, '30100000', '00', 0, 'TestItem', null, to_date('01-JAN-12','DD-MON-YY'), null,'test.user', null);
insert into IC_ITEM_CLASS_T(SEQ_NO_IC,SEQ_NO_UNSP, SEQ_NO_CLASS,USERID, DELETE_DATE) VALUES (5000009,(select SEQ_NO_UNSP from IC_UNSPSC_T where UNSPSC_CODE='30100000' and TITLE='TestItem') , (select SEQ_NO_CLASS from TIM_CLASS_T  where TAX_CLASS='S' and TAX_SUB_CLASS='LOGISTICS'),'test.user', null);
insert into IC_UNSPSC_T (SEQ_NO_UNSP,SEQ_NO_PARENT, UNSPSC_CODE, UNSPSC_BF, LEVEL_NUMBER, TITLE, DESCRIPTION, VALID_FROM, VALID_TO,userid, DELETE_DATE)  values (5000010,null, '40100000', '00', 0, 'TestItem', null, to_date('01-JAN-12','DD-MON-YY'), null,'test.user', null);
insert into IC_ITEM_CLASS_T(SEQ_NO_IC,SEQ_NO_UNSP, SEQ_NO_CLASS,USERID, DELETE_DATE)  VALUES (5000010,(select SEQ_NO_UNSP from IC_UNSPSC_T where UNSPSC_CODE='40100000' and TITLE='TestItem') , (select SEQ_NO_CLASS from TIM_CLASS_T  where TAX_CLASS='S' and TAX_SUB_CLASS='STORAGE'),'test.user', null);
