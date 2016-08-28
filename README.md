# The Galactic Dependencies Treebanks 1.0 Implementation of the paper "The Galactic Dependencies Treebanks: Getting More Data by Synthesizing New Languages" by Dingquan Wang and Jason Eisner. TACL 2016### Download The data release is [here](http://dx.doi.org/10.7910/DVN/8ZT5KF), This yields 37 × 38 × 38 = 53,428 languages in total. * To download and extract the entire dataset:        GALACTIC_ROOT=$(pwd) bin/gd-download * For more options such as downloading some specific substrate languages, whether extract after downloading and whether delete the downloaded file after extracting:        bin/gd-download --help### Build* Compile the code from the command line:        mvn compile* To build a single jar with all the dependencies included:        mvn compile assembly:single### Run * To train a permutation model of, for example, NOUN from a given treebank (toy/sample.conllu):        GALACTIC_ROOT=$(pwd) bin/gd-train-permute --input toy/sample.conllu --node N * To permute a given treebank (toy/sample.collu) using the given permuatation models to a synthetic language, for example, en~fr@N~hi@V:         GALACTIC_ROOT=$(pwd) bin/gd-translate --input toy/sample.conllu --spec en~fr@N~hi@V* For more options, please use:        bin/gd-train-permute --help        bin/gd-translate --helpNote: The given model files are generated from a slightly older [Pacaya](https://github.com/mgormley/pacaya) version, which is no longer used in the current release. So the models reproduced from the current version might be slightly different from what are given. ### Reference ```latex@article{galactic16,    author = {Dingquan Wang and Jason Eisner},    title = {The {G}alactic {D}ependencies Treebanks: Getting More Data by Synthesizing New Languages},    journal = {Transactions of the ACL},    year = {2016},    note = {In review}}