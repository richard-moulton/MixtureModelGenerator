# MixtureModelGenerator
Three real-valued data stream generators based on mixture models: MixtureModelGenerator, MixtureModelGeneratorDrift and MixtureModelGeneratorOC. This approach is inspired by Webb et al.'s Bayesian Network structured [categorical data generator](http://dx.doi.org/10.5281/zenodo.35005) [2]. 

This project has a DOI: [![DOI](https://zenodo.org/badge/102765356.svg)](https://zenodo.org/badge/latestdoi/102765356)

## Compatibility
Designed to be compatible with the MOA (Massive Online Analysis) 17.06 release. MOA [1] is a Java-based, open source framework for data stream mining. More details can be found on its website (https://moa.cms.waikato.ac.nz/) and it can be found on GitHub as well (https://github.com/waikato/moa).

## Dependencies
These classes include the Apache Commons Mathematics Library as a dependency. The Apache Commons Mathematics Library is a library of lightweight, self-contained mathematics and statistics components addressing the most common problems not available in the Java programming language or Commons Lang. More details as well as download links can be found on the library's [website](https://commons.apache.org/proper/commons-math/).

## Generators
1. *MixtureModelGenerator*: Generates a data stream based on an underlying mixture model;
2. *MixtureModelGeneratorDrift*: Generates a data stream with concept drift. The concepts before and after the drift period are each based on an underlying mixture model; and
3. *MixtureModelGeneratorOC*: Generates a data stream representing one majority class and some number of minority classes based on an underlying mixture model. For use with MOA's Imbalanced Stream generator.

## References
[1] Bifet, A., Holmes, G., Kirkby, R. & Pfahringer, B. Moa: Massive online analysis. J. Mach. Learn. Res. 11, 1601–1604 (2010).

[2] Webb, G. I., Hyde, R., Cao, H., Nguyen, H. L. & Petitjean, F. Characterizing concept drift. Data Min. Knowl. Discov. 30, 964–994 (2016).
