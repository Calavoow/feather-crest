Feather Crest
=============
Feather Crest is a Scala library for accessing the EVE Online CREST API.
It will target the JVM language, thus also work for Scala, Java, Clojure and other JVM languages.
The main supported languages, however, will be Scala and Java.

## Interface
With Feather Crest you will be able to use the CREST API as if it were a local asynchronous data object.
Requests to the API are sent asynchronously using Futures,
which allow the user to easily manage a parallel environment.
If you are not familiar with Futures, there is [an official Scala document](http://docs.scala-lang.org/overviews/core/futures.html) explaining them.
This kind of approach is necessary,
because it must not be the case that the application blocks on every API request,
plus it enables sending many different request simultaneously.

The CREST is modelled using small data classes, into which API results are stored.
When these classes are instanced, the data is checked against the model typing.
This strong typing has a lot of benefits over a dynamic typing implementation,
but let us not hold this discussion here.

A unique feature of the CREST are the links to other resource, using internal links.
This is modeled using the `CrestLink` class which mainly provides a `follow` method to follow these links.
When the `follow` method is called, the linked model is instanced.
This in turn allows further traversal through the CREST API,
which is closely related to the intended interaction method of CREST.

## Examples
Because of static typing we can navigate the interface in a checked manner.
A session should usually start with fetching the Root object.
```scala
// Get an ExecutionContext for asynchronous operations.
import scala.concurrent.ExecutionContext.Implicits.global

val root = Root.fetch(None) // Future[Root]
val endpointHref = root.map(_.crestEndpoint.href) // Future[String]
// Let's print the contents of the Future.
endpointHref.foreach(println)
// Asynchonrously prints "https://crest-tq.eveonline.com/"
```

We can convert this to blocking code using the `Await` construct,
which will get the result from the `Future` synchronously.
```scala
import scala.concurrent.Await
import scala.concurrent.duration._

val root = Root.fetch(None) // Future[Root]
// Block for at most 10 seconds to get the Root.
val rootResult = Await.result(root, 10 seconds) // Root
// Then print the href to the endpoint
println(rootResult.crestEndpoint.href)
```

### Regions
For the following examples an `auth : Some[String]` variable is assumed,
which contains a string of the authentication token used to authenticate to the CREST API.

We can get region information by following a link to Regions of the Root class.
Using Scala for-constructs (syntactic sugar for `map` and `flatMap`),
that looks as follows
```scala
for(
	root <- Root.fetch(); // No authentication needed for Root.
	region <- rootRes.regions.follow(auth) // Follow a CrestLink to Regions
) {
	// Asynchronously print the name of all regions
	println(region.items.map(_.name).mkString(", "))
}
```

### Item Types
We give another example, but now of fetching item types:
```scala
// Conversions for Twitter Future and Try to Scala types.
import feather.crest.api.TwitterConverters._

for(
	r <- Root.fetch();
	// Follow the link the the itemTypes page
	itemTypes <- r.itemTypes.follow(auth);
	/**
	 * The itemtypes are split over multiple pages (there are 30k+ of them),
	 * thus we create an asynchronous collection over all itemtype pages
	 * (a Twitter [[com.twitter.concurrent.Spool]]), and concatenate all items.
	 **/
	allTypes <- twitterToScalaFuture(
		itemTypes.paramsIterator(auth, retries=2)
			.map(_.items)
			.reduceLeft(_ ++ _)
	)
) {
	// Let's print the first and last item type
	println(allTypes.head)
	println(allTypes.last)
}
```
The key feature here is the collection that is returned by `authedIterator`, the `Spool`.
It is an asynchronous variant of the `Stream`, which iterates through pages of the CREST and stores the results.
This kind of construct is necessary, because the number of item types is over 20,000
such that CCP has split the item types over multiple pages / requests.
We decided not to pull all pages all the time, to allow the user full control over the requests.

## Market Orders (using `scala-async`)
Another excellent approach for handling Futures is using [scala-async](https://github.com/scala/async).
One important feature of the CREST, is the availability of very recent market data
that is updated every 5 minutes.
As an example we get the market information of the Hammerhead II item.
```scala
// This will contain our Hammerhead II buy and sell orders.
val ham2Orders : Future[(MarketOrders, MarketOrders)] = async {
	// Note: no Futures! But everything outside async will stil be asynchronous.
	val aRoot: Root = await(Root.fetch())
	val regions: Regions = await(aRoot.regions.follow(auth))
	// Note that I use {{.get}} here, which could throw an exception,
	// but simplifies this example.
	val forge: Region = await(regions.items.find(_.name == "The Forge").get
		.follow(auth))

	/**
	 * From the type of [[Region.marketSellLink]]
	 * we see that we need an CrestLink[ItemType],
	 * so lets handle that in _parallel_.
	 *
	 * Async-await will automatically find independent asynchronous requests,
	 * and run them in parallel.
	 */
	val itemTypes : ItemTypes = await(aRoot.itemTypes.follow(auth))
	val ham2Link = itemTypes.items.find(_.name == "Hammerhead II").get


	// Now we put everything together and get the buy and sell orders.
	val ham2Buy : MarketOrders = await(forge.marketBuyLink(ham2Link)
		.follow(auth))
	val ham2Sell : MarketOrders = await(forge.marketSellLink(ham2Link)
		.follow(auth))

	(ham2Buy, ham2Sell)
}
```

###Where to find more examples
Look in the `src/test/scala` folder for some running examples.
They look slightly different than the previous examples, because of the way test suites handles Futures.
It pays off to get used to `map` and `flatMap` first, so you know how things work,
and then switch to for-constructs (which just use `map` and `flatMap`)
or `scala-async` for ease of use and readability.

## Implementation
Models that have been implemented can be found in `src/main/scala/feather/crest/models`.
Since adding models is relatively simple, please do suggest missing models,
as identified by `TodoCrestLink`.

1. Character
1. Corporation
	1. Alliances
	1. Campaigns
	1. Corporations
	1. Structures
	1. Wars
1. Industry
	1. Facilities
	1. Systems
1. Item Types
	1. Categories
	1. Groups
	1. Types
1. Market
	1. Groups
	1. Orders
	1. History
	1. Prices
1. Other
	1. Killmails
	1. Tournaments
1. Root CREST page
1. Universe
	1. Planet
	1. Regions
	1. Solar System

## Todo List 
- [ ] Modelling the entire CREST API.
- [ ] Creating tests for all models.
- [ ] Caching of API requests.
	- [x] Implementation
	- [ ] Tests
- [ ] Improve interface.
	- [ ] Authentication token.
	- [ ] Hide disallowed methods.
	- [x] Asynchronous iterator.
