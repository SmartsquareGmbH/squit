update cbd_tax_rate_t set valid_to=TO_DATE('01-JAN-07', 'DD-MON-RR') where tax_code=0 and tax_rate=19 and ga_code='DE' and valid_from=TO_DATE('01-JAN-07', 'DD-MON-RR');
