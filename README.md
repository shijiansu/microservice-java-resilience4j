![](https://img.shields.io/badge/language-java-blue)
![](https://img.shields.io/badge/technology-resilience4j,%20rate%20limiting,%20circuit%20breaker-blue)
![](https://img.shields.io/badge/development%20year-2020-orange)
![](https://img.shields.io/badge/contributor-shijian%20su-purple)
![](https://img.shields.io/badge/license-MIT-lightgrey)

![](https://img.shields.io/github/languages/top/shijiansu/microservice-java-resilience4j)
![](https://img.shields.io/github/languages/count/shijiansu/microservice-java-resilience4j)
![](https://img.shields.io/github/languages/code-size/shijiansu/microservice-java-resilience4j)
![](https://img.shields.io/github/repo-size/shijiansu/microservice-java-resilience4j)
![](https://img.shields.io/github/last-commit/shijiansu/microservice-java-resilience4j?color=red)
![](https://github.com/shijiansu/microservice-java-resilience4j/workflows/ci%20build/badge.svg)

--------------------------------------------------------------------------------

- resilience4j-first-try - examples of first try - TODO - include examples of resilience4j official examples
  - resilience4j-first-try-common - examples of common classes use for all below projects
  - resilience4j-first-try-circuitbreaker - examples of circuit breaker (trigger by Exception)
  - resilience4j-first-try-retry - examples of retry (trigger by Exception)
  - resilience4j-first-try-ratelimiter - examples of rate limiter (control on request #)
  - resilience4j-first-try-bulkhead - examples of bulkhead (control on Thread #)
  - resilience4j-first-try-timelimiter - examples of time limter (control on Future process time)
- resilience4j-by-taoyangui

--------------------------------------------------------------------------------

# How to learn

主要参考官网, 官网中有对应的参数文档和例子.

- https://resilience4j.readme.io/v0.15.0/docs/circuitbreaker
- https://resilience4j.readme.io/v0.15.0/docs/bulkhead
- https://resilience4j.readme.io/v0.15.0/docs/ratelimiter
- https://resilience4j.readme.io/v0.15.0/docs/retry

并且有对应的集成项目, 例如<https://resilience4j.readme.io/v0.15.0/docs/getting-started-3>

监控, 整合了`Micrometer`和`Grafana`, 也在官网中有介绍

# Introduction

- Resilience4j
  - it has dependencies of `io.vavr.vavr`, which is a enhanced functional programming library. 
- Circuit Breaker
  - CLOSED ==> OPEN: 单向转换. 当请求失败率超过阈值时, 熔断器的状态由关闭状态转换到打开状态. 失败率的阈值默认50%, 可以通过设置CircuitBreakerConfig实例的failureRateThreshold属性值进行改变.
  - OPEN <==> HALF_OPEN: 双向转换. 打开状态的持续时间结束, 熔断器的状态由打开状态转换到半开状态. 这时允许一定数量的请求通过, 当这些请求的失败率超过阈值, 熔断器的状态由半开状态转换回打开状态. 半开时请求的数量是由CircuitBreakerConfig实例的ringBufferSizeInHalfOpenState属性值设置的.
  - HALF_OPEN ==> CLOSED: 如果请求失败率小于阈值, 则熔断器的状态由半开状态转换到关闭状态.

--------------------------------------------------------------------------------

# Execute all tests in repo

`/bin/bash run-repo-test.sh`

