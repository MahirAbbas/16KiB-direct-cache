Explainer of all the Paramaters


cacheSize: Int,
wayCount: Int,
refillCount: Int, 
writebackCount: Int, 
cpuDataWidth: Int, 
withCoherency : Boolean = false,
loadRefillCheckEarly : Boolean = true,
storeRefillCheckEarly : Boolean = true
lineSize : Int, 
loadReadBanksAt: Int,
loadReadTagsAt: Int,
loadHitsAt: Int,
loadHitAt: Int,
loadBankMuxesAt: Int,
loadBankMuxAt: Int,
laodControlAt: Int,
loadRspAt: Int,
storeReadBanksAt: Int,
storeReadTagsAt: Int,
storeHitsAt: Int,
storeHitAt: Int,
storeControlAt: Int,
storeRspAt: Int,
tagsReadAsync: Boolean,
reducedBankWidth : Boolean = false) 


### Core Cache Configuration
cacheSize: The total size of the cache in bytes. This determines how much data the cache can hold.
wayCount: The number of ways in a set-associative cache. This affects the cache's associativity, which is a key factor in its hit rate.
refillCount: The number of data units (e.g., words, bytes) fetched during a cache refill operation, typically after a cache miss.
writebackCount: The number of data units written back to the lower memory level when a cache line is evicted and needs to be saved.
cpuDataWidth: The width of the data bus between the CPU and the cache, usually in bits. This affects the maximum size of data that can be transferred in one operation.
lineSize: The size of a cache line in bytes. Each line is the smallest unit of data that can be transferred between the cache and main memory.


### Performance and Coherency
withCoherency: Indicates whether the cache supports coherency protocols to maintain data consistency across multiple caches (e.g., in a multicore system).
loadRefillCheckEarly and storeRefillCheckEarly: Determines whether the cache checks for the need to refill (on load or store operations) early in the pipeline, potentially improving latency by starting refill operations sooner.

### Pipeline Stages
These parameters specify at what pipeline stage various cache operations occur, such as reading data, checking tags for hits/misses, and responding to load or store requests. The stages are typically denoted by clock cycles:
loadReadBanksAt, storeReadBanksAt: When the data banks are read during load and store operations, respectively.
loadReadTagsAt, storeReadTagsAt: When the tag array is read to determine hits or misses.
loadHitsAt, storeHitsAt: When it's determined whether a load or store operation hits in the cache.
loadHitAt, storeHitAt: Likely a typo for loadHitsAt and storeHitsAt, or it could specifically denote when the final hit determination is made.
loadBankMuxesAt, loadBankMuxAt: When data from different banks is selected (muxed) for loads. The two parameters might refer to the setup of bank muxes and the actual selection, respectively.
laodControlAt: Likely a typo, intended to be loadControlAt, indicating when control logic for load operations is processed.
loadRspAt, storeRspAt: When the cache responds to load and store requests, respectively.

### Operational Characteristics
tagsReadAsync: Indicates if tag array reads are performed asynchronously, which can impact the speed and complexity of the cache design.
reducedBankWidth: Whether the cache uses a reduced-width data path for the banks, potentially saving area at the cost of possibly increased access latency.




### Banks Generation




### WaysWrite generation



### Ways generation



