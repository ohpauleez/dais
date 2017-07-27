# Dais

`dais` is an experiment to create a single-threaded, synchronous-only
Interceptor chain directly in Java, while maintaining the general API
and contracts of Pedestal's Interceptor Chain.

The Dais Chain is purposefully simple, with no internal logging, metrics, or
third-party dependencies.  It's designed to be as slim and "bare-bones" as
possible and focused on resource-constrained applications.

## Usage

Please see `Example.java` and `server.clj`

## License

Copyright © 2017 Paul deGrandis

Distributed under the Eclipse Public License version 1.0.

