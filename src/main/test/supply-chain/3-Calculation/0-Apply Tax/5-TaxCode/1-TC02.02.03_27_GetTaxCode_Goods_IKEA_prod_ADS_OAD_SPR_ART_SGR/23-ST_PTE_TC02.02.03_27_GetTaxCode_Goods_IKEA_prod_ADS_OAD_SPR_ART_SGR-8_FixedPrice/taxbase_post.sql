update rix_tax_item_t set tax_code=0 where item_code='67228700' and ITEM_TYPE='ADS' and ga_code='DE';
update rix_tax_item_t set tax_code=0 where item_code='50010191' and ITEM_TYPE='OAD' and ga_code='DE';
update rix_tax_item_t set tax_code=0, delete_date=to_date('10-SEP-12','DD-MON-RR') where item_code='29881718' and ITEM_TYPE='SPR' and ga_code='BE';
update rix_tax_item_t set tax_code=0 where item_code='90136612' and ITEM_TYPE='ART' and ga_code='DE';
update rix_tax_item_t set tax_code=0 where item_code='30000211' and ITEM_TYPE='SGR' and ga_code='DE';