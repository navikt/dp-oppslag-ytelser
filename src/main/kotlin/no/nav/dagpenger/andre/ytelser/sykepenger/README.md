# Sykepenger


Repo: https://github.com/navikt/helse-spokelse/

spokelse er en tjeneste som svarer på ytelser om:

- Sykepenger etter lovens kapittel 8


Request/response: 

````json
{
  "endepunkt": "POST /utbetalte-perioder-aap",
  "request": {
    "personidentifikatorer": ["11111111111"],
    "fom": "2018-01-01",
    "tom": "2018-03-31"
  },
  "response": {
    "utbetaltePerioder": [
      { "fom": "2018-01-01", "tom": "2018-02-10", "grad": 100 },
      { "fom": "2018-03-01", "tom": "2018-03-31", "grad": 50 }
    ]
  }
}

````
