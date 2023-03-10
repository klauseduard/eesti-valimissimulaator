## 2023-03-04

### Added

  * Fix: make sure no leftover votes remain when dividing party votes between
  candidates.

  * New feature: add ability to pass command-line instruction to use Emor's
  poll data instead of the default Norstat data. (Emor's data is hardcoded
  in a Kotlin file.)

  * Fix: examples/probabilities-2023-03-03.txt [updated with correct
  run data]

## 2023-03-03

### Added

  * New data: pollster data from reitingud.ee --
  src/main/resources/reitingud-2023-03-03.json

  * New calculations: examples/probabilities-2023-03-03.txt [wrong run data though!]



## 2023-03-01

### Added

  * New data: pollster data from reitingud.ee --
  src/main/resources/reitingud-2023-02-27.json

  * New calculations: examples/probabilities-2023-02-27.txt



## 2023-02-23

### Added

  * New feature: Party ratings are now obtained from a JSON file that is derived
  from the output of `reitingud.ee`. This modification should simplify the process
  of updating ratings in the future. Nonetheless, the initial ratings from ERR
  News are still present in Andmed.kt. It is deemed necessary to have the functionality
  to pass the ratings source to the program as a parameter when running it. However,
  please note that this feature has not yet been implemented. There is a significant
  disparity in the data obtained from various pollsters, which in turn leads to a
  notable divergence in the proportion of seats that each party receives.

  * New data: pollster data from reitingud.ee --
  src/main/resources/reitingud-2023-02-23.json
   
  * New calculations: examples/probabilities-2023-02-23.txt

  * TODO: No time for cleanup this time.
