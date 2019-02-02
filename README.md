# Dais

`dais` is a minimal Interceptor Chain implementation written in
Java and JavaScript.
It's focus is to allow single-threaded, synchronous-only Chains to perform
with as little overhead as possible, while also enabling custom Chains to enhance
capabilities (for example, adding asynchronous interceptors).

Dais attempts to maintain the general API and contracts of the [Pedestal's](https://github.com/pedestal/pedestal)
Interceptor Chain.

The core of the Dais Chain is purposefully simple, with no internal logging, metrics, or
third-party dependencies.  It's designed to be as slim and "bare-bones" as
possible and focused on resource-constrained applications.

Other Dais modules build upon this simple core with additional functionality and costs/trade-offs.

## Usage

TODO: This needs to be refeshed in the new setup
Please see `Example.java` and `server.clj`

## License

Copyright © 2017 Paul deGrandis

Distributed under the [Eclipse Public License version 1.0](https://opensource.org/licenses/EPL-1.0).

Parts of the additional modules are provided in part by [Pedestal](https://github.com/pedestal/pedestal),
released under the [EPL v1.0](https://opensource.org/licenses/EPL-1.0)

JavaScript base logging as found in the ClojureScript code is provided by
[loglevel](https://github.com/pimterry/loglevel), released under the [MIT License](https://opensource.org/licenses/MIT).
