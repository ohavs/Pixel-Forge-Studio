import sys, xmlschema
xs = xmlschema.XMLSchema11(sys.argv[1])
for f in sys.argv[2:]:
    errs = list(xs.iter_errors(f))
    print(f.split('/')[-1], 'OK' if not errs else '')
    for e in errs[:4]: print('   ', e.reason, '|', e.path)
