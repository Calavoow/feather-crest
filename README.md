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

Furthermore, there is a rate limit on the number of parallel connections,
internally feather-crest limits the number of parallel connections to the maximum of 20.
So you don't have to worry about exceeding the connection limit by setting up many parallel requests,
feather-crest does that for you.

## Installation
The installation mainly consists of installing SBT, a build tool for Scala projects.
[The installation instructions](http://www.scala-sbt.org/release/docs/Setup.html) are publicly available
for Linux, Mac and Windows or any system that runs the Java Virtual Machine.
For tests involving the Authed CREST, it is required to create a file `src/test/resources/auth.txt`
with just the authentication token in it, otherwise you will get errors when authenticating.
Once installed you can compile or test the project through
```sh
$ cd feather-crest
$ sbt
> compile
> test
```
At least until this is published to Maven Central,
you can install the library locally to make it available as dependency with
```sh
$ sbt
> publishLocal
```

And then add a dependency with
```
libraryDependencies += "eu.calavoow" %% "feather-crest" % "0.2"
```

## Examples
Because of static typing we can navigate the interface in a checked manner.
A session should usually start with fetching the Root object.
```scala
// Get an ExecutionContext for asynchronous operations.
import scala.concurrent.ExecutionContext.Implicits.global

val root = Root.public(None) // Future[Root]
val endpointHref = root.map(_.crestEndpoint.href) // Future[String]
// Lets print the contents of the Future.
endpointHref.foreach(println)
// Asynchonrously prints "https://public-crest.eveonline.com/"
```

We can convert this to blocking code using the `Await` construct,
which will get the result from the `Future` synchronously.
```scala
import scala.concurrent.Await
import scala.concurrent.duration._

val root = Root.public(None) // Future[Root]
// Block for at most 10 seconds to get the Root.
val rootResult = Await.result(root, 10 seconds) // Root
// Then print the href to the endpoint
println(rootResult.crestEndpoint.href)
```

But with some practice and the tips below you will find out that Futures are not an obstacle at all,
rather they very easily allow asynchronous behaviour.

### Regions
We can get region information by following a link to Regions of the public CREST Root object.
Using Scala for-constructs (syntactic sugar for `map` and `flatMap`),
that looks as follows
```scala
for(
	root <- Root.public(); // Fetch the public Root
	// Follow a CrestLink to Regions
	region <- rootRes.regions.follow(auth)
) {
	// Asynchronously print the name of all regions
	println(region.items.map(_.name).mkString(", "))
}
```

### Item Types
We give a more advanced example, but now of fetching item types from the public CREST.
Item types are spread over multiple pages, because there are many of them.
Feather-crest models this using a `Stream[Future[T]]` where `Stream` is like a lazy list,
which will only fetch the pages that have been requested from the API.

```scala
for(
	r <- Root.public();
	// Construct a stream of ItemTypes
	// and convert `Stream[Future[ItemType]]` to `Future[Stream[ItemType]]`
	itemTypes <- Future.sequence(r.itemTypes.construct());
) {
	// Lets print the first item type and the number of item types.
	val list = itemTypes.flatMap(_.items)
	println(list.head)
	println(list.size)
}
```

The key feature here is the collection returned by `r.itemTypes.construct()`,
which is the `Stream` as explained before.
With the use of the standard library function `Future.sequence` we can easily
get rid of the `Future`s inside the stream.
Unfortunately, the `Future.sequence` function is eager by default and will request all pages,
refer to [this stackoverflow issue](http://stackoverflow.com/questions/18043749/mapping-a-stream-with-a-function-returning-a-future)
for more details.

## Market Orders (using `scala-async`)
Another excellent approach for handling Futures is using [scala-async](https://github.com/scala/async).
One important feature of the CREST, is the availability of very recent market data
that is updated every 5 minutes.
As an example we get the market information of the Hammerhead II item.
```scala
// This will contain our Hammerhead II buy and sell orders.
val ham2Orders : Future[(MarketOrders, MarketOrders)] = async {
	// Note: no Futures! But neither will the outer thread block.
	val aRoot: Root = await(Root.public())
	val regions: Regions = await(aRoot.regions.follow())
	val forge: Region = await(regions.items.find(_.name == "The Forge").get
		.follow())

	/**
	 * From the type of [[Region.marketSellLink]]
	 * we see that we need an CrestLink[ItemType],
	 * so lets handle that in _parallel_.
	 *
	 * Async-await will automatically find independent asynchronous requests,
	 * and run them in parallel.
	 *
	 * Note: I know that the item is on the first page of itemtypes,
	 * this saves me a little time and simplifies things.
	 **/
	val itemTypes = await(aRoot.itemTypes.construct().head)
	// Then we find the Hammerhead II item in the list.
	val ham2Link = itemTypes.items.find(_.name == "Hammerhead II").get


	// Now we put everything together and get the buy and sell orders.
	val ham2Buy : MarketOrders = await(forge.marketBuyLink(ham2Link)
		.follow())
	val ham2Sell : MarketOrders = await(forge.marketSellLink(ham2Link)
		.follow())

	(ham2Buy, ham2Sell)
}
```

### Authed CREST example
The Authed CREST has some possibilities that the public CREST doesn't.
In general, it is advisible to use the authed CREST only when you need to
and th public CREST otherwise, because of caching on CCP's side of things.
For this example we fetch the location of a character.
```scala
// Contains the authentication token, obtained through EVE SSO.
val auth : Some[String] = ...
val loc = for(
	r <- Root.authed(); // Note the authed
	decode <- r.decode.follow(auth);
	char <- decode.character.follow(auth);
	location <- char.location.follow(auth)
) yield {
	// When a user isn't logged in the information here is empty.
	val system = location.solarSystem
		.getOrElse(println("User is not logged into the game")
	println(system.name)
}
```

Also note that all public CREST features are also available on the private CREST,
but the links used are different.
So do not mix up `CrestLink` classes obtained from different CREST endpoints,
when required as parameters for for example the market buy and sell orders.

### Where to find more examples
Look in the `src/test/scala` folder for some running examples.
They look slightly different than the previous examples, because of the way test suites handles Futures.
It pays off to get used to `map` and `flatMap` first, so you know how things work,
and then switch to for-constructs (which just use `map` and `flatMap`) or `scala-async`.
Also have a look at a [project using feather-crest](https://github.com/Calavoow/BringMeTo),
which is a website that uses many of the features of feather-crest to route players.

## Implementation
Models that have been implemented can be found in `src/main/scala/feather/crest/models`.
Since adding models is relatively simple, please do suggest missing models,
as identified by `TodoCrestLink`.
A selection of models that have been implemented:

1. Root CREST page
1. Character
	1. Location
	1. Set waypoints
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
1. Universe
	1. Planets
	1. Regions
	1. Solar Systems

## Todo List 
- [ ] Modelling the entire CREST API.
- [ ] Creating tests for all models.
- [ ] Caching of API requests.
	- [x] Implementation
	- [ ] Tests
- [x] Improve interface.
	- [x] Authentication token.
	- [x] Asynchronous iterator.
