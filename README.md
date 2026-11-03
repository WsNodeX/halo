<div align="center">

# Halo

### A zero-dependency semantic retrieval engine for the JVM

Sub-millisecond hybrid search (dense + sparse), a from-scratch HNSW index,
BM25 lexical ranking, reciprocal-rank fusion, feature re-ranking and a complete
retrieval-augmented-generation pipeline — all in pure Java, no native code,
no external services.

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Zero Dependencies](https://img.shields.io/badge/dependencies-0-blue.svg)]( )
[![Platform](https://img.shields.io/badge/platform-JVM%20%2F%20any-9cf)](https://en.wikipedia.org/wiki/Java_virtual_machine)
[![Tests](https://img.shields.io/badge/tests-43%20passing-brightgreen.svg)](src/test)
[![CI](https://img.shields.io/badge/CI-GitHub%20Actions-2088ff.svg)](.github/workflows/ci.yml)

</div>

<p align="center">
  <a href="#-why">Why</a> ·
  <a href="#-features">Features</a> ·
  <a href="#-install">Install</a> ·
  <a href="#-quick-start">Quick Start</a> ·
  <a href="#-architecture">Architecture</a> ·
  <a href="#-api">API</a> ·
  <a href="#-roadmap">Roadmap</a> ·
  <a href="#-contributing">Contributing</a> ·
  <a href="#中文解读从零实现一个语义检索引擎">中文解读</a>
</p>

---

## ✨ Why

Search is the invisible backbone of almost every product: docs, support, code,
e-commerce, and of course every LLM application. Yet most teams treat it as a
black box they rent from a managed service.

**Halo** exists so you can *own* that box. It is written from first principles
in standard Java:

- **No dependencies.** Drop the source into any JDK 21 project, or compile it
  with `javac` alone. There is nothing to pull, nothing to patch, nothing that
  breaks when a transitive dependency publishes a breaking release.
- **No network calls.** Every embedder, index, reranker and the built-in LLM
  stub run entirely offline. You can unit-test retrieval quality on a plane.
- **Both signals, fused.** Dense vectors catch paraphrases; lexical BM25 catches
  exact keywords. Halo fuses them with Reciprocal Rank Fusion so you stop
  choosing between "semantic" and "keyword" search.
- **A real ANN index.** The HNSW graph is implemented by hand, the same
  algorithm that powers the fastest vector databases — readable in a few hundred
  lines instead of buried inside a C++ extension.

If you are preparing for backend or AI-infra interviews, Halo is also a compact
tour of the ideas that actually get asked: cosine similarity, inverted indices,
BM25, hierarchical graphs, learning-to-rank and RAG.

## 🚀 Features

- **Multiple offline embedders** — hashing-trick, TF-IDF, bag-of-words and
  character n-gram encoders. Deterministic, trainable on your own corpus, and
  free of any model download.
- **Dense index** — exact `FlatIndex` (brute force, used as ground truth) and an
  approximate `HnswIndex` built from scratch with configurable `M`,
  `efConstruction` and `efSearch`.
- **Sparse index** — a `InvertedIndex` with full Okapi **BM25** ranking.
- **Hybrid retrieval** — `HybridIndex` combines dense + sparse via **Reciprocal
  Rank Fusion (RRF)**, robust to differently-scaled scores.
- **Re-ranking** — a `FeatureReranker` that min-max normalises each signal and
  combines them with a transparent weighted sum (the core idea of
  learning-to-rank).
- **RAG pipeline** — wire a `Collection` to any model exposing
  `complete(query, passages)` and get grounded answers with inline citations.
- **Persistence & evaluation** — serialise a corpus to JSON and reload it;
  measure `recall@k` and `MRR@k` offline.
- **Small, layered, testable** — 43 unit tests cover math, tokenisation, every
  index, fusion, re-ranking, persistence round-trips and the CLI.

## 📦 Install

Halo is plain Java source — no build tooling required.

```bash
# requires JDK 21
git clone https://github.com/WsNodeX/halo.git
cd halo
javac -d build/main $(find src/main/java -name '*.java')
```

To run the tests you only need the standard JUnit 5 standalone launcher:

```bash
javac -cp "build/main:junit-platform-console-standalone-1.10.2.jar" \
      -d build/test $(find src/test/java -name '*.java')
java -cp "build/main:build/test:junit-platform-console-standalone-1.10.2.jar" \
     org.junit.platform.console.ConsoleLauncher --scan-class-path build/test
```

## ⚡ Quick Start

Run the built-in showcase (no data files, no API keys):

```bash
java -cp build/main halo.examples.Demo
```

Expected output:

```text
== Hybrid retrieval ==
query: How does garbage collection work in the JVM?
  jvm-gc#0        dense=0.230 sparse=3.614 fused=0.033
  vector-search#0  dense=0.090 sparse=1.339 fused=0.032
  btree#0          dense=0.076 sparse=0.154 fused=0.032

== Retrieval-augmented generation ==
Based on the retrieved material, the answer is:

The JVM garbage collector reclaims objects that are no longer reachable, so
developers do not free memory by hand. [1]

Vector search finds items whose embedding is close to a query embedding. [2]

== Offline evaluation ==
  recall@3=1.000  mrr@3=1.000  (n=3)
```

Programmatic usage:

```java
import halo.embed.HashEmbedder;
import halo.store.Collection;
import halo.store.Document;

Collection kb = new Collection("notes", new HashEmbedder(1024));
kb.add(new Document("gc", "The JVM garbage collector reclaims unreachable objects."));
kb.add(new Document("raft", "Raft elects a leader that replicates a consistent log."));

kb.search("JVM garbage collection", 3)
   .forEach(r -> System.out.println(r.id() + " " + r.score()));
```

A command-line front-end is also included:

```bash
java -cp build/main halo.cli.Cli version
java -cp build/main halo.cli.Cli demo
java -cp build/main halo.cli.Cli index   ./docs   ./kb.json
java -cp build/main halo.cli.Cli query   ./kb.json "how does raft elect a leader" 5
java -cp build/main halo.cli.Cli ask     ./kb.json "explain garbage collection" 3
java -cp build/main halo.cli.Cli eval    ./kb.json ./queries.json
```

## 🏗️ Architecture

```
            ┌──────────────┐
 query  ──▶ │  Tokenizer   │  (word / char-ngram / word-ngram)
            └──────┬───────┘
                   │
           ┌───────┴────────┐
           │  Dual Encoder  │  HashEmbedder · TfIdf · BagOfWords · Ngram
           └───────┬────────┘
                   │  dense vector  ──▶ ┌──────────────────────┐
                   │                    │ DenseIndex            │
                   │                    │  ├ FlatIndex (exact)  │
                   │                    │  └ HnswIndex (ANN)    │
                   └──▶ sparse tokens ─▶│ InvertedIndex (BM25)  │
                                        └──────────┬───────────┘
                                                   │  RRF fusion
                                           ┌───────┴────────┐
                                           │  Re-ranker     │  FeatureReranker
                                           └───────┬────────┘
                                                   │  ranked passages
                                           ┌───────┴────────┐
                                           │  RAG Engine    │  model.complete(q, hits)
                                           └───────┬────────┘
                                                   ▼
                                          answer + citations
```

Every box is an interface you can swap: plug in a transformer-backed encoder
for production-grade semantics, or a real OpenAI-compatible client instead of
the offline `StubLlm`.

## 🔌 API

| Component | Key method | Responsibility |
|---|---|---|
| `Embedder` | `float[] embed(String)` | Map text → fixed-dim vector |
| `HashEmbedder` | `embed(text)` | Hashing-trick dense vector, no training |
| `TfIdfEmbedder` | `fit(corpus)`, `embed(text)` | Trained TF-IDF weighting |
| `VectorIndex` | `add(id, vec)`, `search(vec, k)` | Dense nearest neighbours |
| `HnswIndex` | `add`, `search`, `setEfSearch` | Approximate NN graph |
| `InvertedIndex` | `add(id, text)`, `search(q, k)` | BM25 lexical ranking |
| `HybridIndex` | `add(id, text)`, `search(q, k)` | Dense + sparse fused via RRF |
| `FeatureReranker` | `rerank(query, candidates)` | Weighted multi-signal re-rank |
| `Collection` | `add`, `search`, `save`, `load` | Chunked, persisted corpus |
| `RagEngine` | `ask(query)` | Retrieval → answer + citations |
| `Evaluator` | `evaluate(collection, queries, k)` | recall@k / MRR@k |

## 🧭 Roadmap

- [ ] Quantized (`int8`) vectors to shrink the HNSW memory footprint
- [ ] On-disk segment storage for billion-scale corpora
- [ ] A transformer-backed encoder adapters (ONNX / external service)
- [ ] Async / streaming ingestion API
- [ ] gRPC and HTTP server front-ends
- [ ] Watched-folder indexing with incremental updates
- [ ] A small web UI for interactive exploration

## 🤝 Contributing

This is a learning-oriented, dependency-free codebase by design: the value is in
keeping every algorithm readable. If you open a pull request, please:

1. Keep new code free of third-party runtime dependencies.
2. Add or extend a unit test that pins the new behaviour.
3. Run the full suite (see Install) and confirm all tests pass.

## 📄 License

Released under the [MIT License](LICENSE). Use it, fork it, ship it.

---

# 中文解读：从零实现一个语义检索引擎

> 下面这篇长文可以直接搬到掘金 / 知乎 / 小红书 / B 站作为技术科普稿。文中所有代码都来自本仓库 `halo`，可以在本地零依赖跑通。

## 一、它解决什么问题

"搜索"几乎是每个产品的隐形地基：文档站、客服、代码仓库、电商，以及今天最火的
**RAG（检索增强生成）** 应用，底层都绕不开一个问题——**给我一段查询，从海量文本里
找出最相关的那几段**。

业界成熟方案很多（Elasticsearch、向量数据库、各种 SaaS），但它们对学习者来说常常是
"黑盒"：你调一个 SDK，拿到结果，却说不清里面发生了什么。本文用一个纯 Java、零依赖的
实现 **Halo**，把这套系统拆成可读的几百行代码，逐一讲清楚。

## 二、核心原理：双路检索 + 融合

单一路子的检索都有短板：

- **稠密检索（向量）**：把文本编码成向量，用余弦相似度找近邻。好处是能理解
  "同义改写"——"JVM 垃圾回收"和"GC 自动内存管理"向量很近。坏处是对**精确关键词**
  （产品型号、错误码、人名）反而迟钝。
- **稀疏检索（关键词）**：经典 BM25，倒排索引 + 词频统计。好处是精确匹配极强；
  坏处是"同义不同词"就抓瞎。

**Halo 的做法是两条路同时走，再用 Reciprocal Rank Fusion（RRF）融合**：

```
score(d) = Σ_lists  1 / (k + rank(d))
```

RRF 不需要把稠密分数和稀疏分数归一化到同一量纲——它只看"每个列表里排第几"，然后
倒数加权求和。这种方法参数极少、极其稳定，是工业界"语义 + 关键词"混合检索的常见打法。

为了工程上能离线验证、能跑单元测试，Halo 内置了**离线向量编码器**（哈希技巧 /
TF-IDF / 字符 n-gram）和一个**离线伪 LLM**——它们不依赖任何模型下载或网络请求，
但对外暴露的接口和"真实 encoder / 真实 LLM"完全同构，替换成本为零。

## 三、关键实现点（面试常问）

**1. 余弦相似度与归一化。** 插入和查询时都做 L2 归一化，这样余弦相似度就等于向量点积，
比较时不用每次开方。哈希技巧编码器把每个 token 用确定性哈希映射到固定维度桶里累加，
再归一化——简单但语义重叠度高的文本向量确实更近。

**2. HNSW 近似最近邻。** 这是 Halo 里最"硬核"的一块。Hierarchical Navigable Small
World 的思路是建一张**多层图**：顶层是稀疏的"高速路"，底层是稠密的全连接，查询时从顶
层贪心往查询点走，用远低于暴力扫描的成本拿到近邻。关键参数：`M`（每层最大连接数）、
`efConstruction`（建图时候选集大小）、`efSearch`（查询时候选集大小，越大召回越高）。
仓库的测试里，随机向量上 HNSW 对比暴力检索的 **recall@10 ≥ 0.85**。

**3. BM25 词频打分。** 不再是朴素 TF-IDF，而是用 `IDF × (f·(k1+1)) / (f + k1·(1−b+b·dl/avgdl))`
——长文档惩罚、词频次线性饱和，这些都是 Elasticsearch 的同款公式。

**4. 特征重排（Learning-to-Rank 的雏形）。** 融合之后还可以再排一轮：把稠密分、稀疏分、
以及任何业务特征（新鲜度、热度、标题命中）各自做 min-max 归一化，再按权重线性相加。
这套"多信号加权"正是排序学习最核心的思想。

**5. RAG 闭环。** 检索 Top-k 段落 → 拼成上下文 → 喂给模型生成答案，并带上 `[1][2]`
这样的**引用标注**。Halo 的伪 LLM 严格只依据检索到的段落作答，方便你把整条链路端到端
跑通、写测试。

## 四、与同类方案对比

| 维度 | Halo（本仓库） | Elasticsearch | 托管向量数据库 |
|---|---|---|---|
| 依赖 | 零，纯 JDK 21 | 重，JVM 集群 | 外部服务 + SDK |
| 是否需要网络 | 否（全离线） | 本地可跑 | 通常需要账号/云 |
| 语义检索 | ✅ 稠密 + 稀疏融合 | ✅（需插件） | ✅ |
| 可阅读性 | 几百行可读源码 | 黑盒 | 黑盒 |
| 规模 | 中小语料 / 学习 / 原型 | 海量 | 海量 |
| 适合场景 | 学习、原型、嵌入式、面试 | 生产搜索 | 生产向量检索 |

一句话：**Halo 不是要替代生产级系统，而是让你真正"拥有"并理解这套系统。**

## 五、如何扩展成生产方案

1. **换编码器**：把 `HashEmbedder` 换成接 ONNX / 外部服务的 Transformer 编码器，
   语义质量会质的飞跃，接口不用改。
2. **换 LLM**：实现 `complete(query, passages)` 同签名的方法接 OpenAI 兼容接口，
   RAG 链路直接复用。
3. **上规模**：给 HNSW 加 `int8` 量化省内存；把 `Collection` 的持久化从 JSON 换成
   分段磁盘存储，支撑上亿级文档。
4. **加服务**：包一层 gRPC / HTTP，前面加鉴权和限流，就是一个最小可用的内部检索引擎。

## 六、如何把本文扩展成一篇发布稿

- **标题方向**：《用 400 行纯 Java 手写一个语义检索引擎，彻底搞懂 RAG 的检索层》
  《面试官问我 HNSW 怎么实现，我把源码读了一遍》
- **配图建议**：架构图（见仓库 README 的 ASCII 图，可改成 draw.io / Excalidraw）、
  RRF 融合示意图、HNSW 多层图手绘。
- **互动钩子**：末尾抛问题——"你觉得语义检索和关键词检索，哪个更适合你的场景？"
- **代码可运行**：文中的所有片段都能在 `halo.examples.Demo` 里直接跑，读者 copy 即验。
- **发布平台**：技术深度版发掘金/知乎，演示动图版发 B 站/小红书，强调"零依赖、能跑通"。
