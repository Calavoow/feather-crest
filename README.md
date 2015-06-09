Feather Crest
=============
Feather Crest is a Scala library for accessing the EVE Online CREST API.
It will target the JVM language, thus also work for Scala, Java, Clojure and other JVM languages.
The main supported languages, however, will be Scala and Java.

## Interface
With Feather Crest you will be able to use the CREST API as if it were a local asynchronous data object.
Requests to the API are sent asynchronously using Futures,
which allow the user to easily manage a parallel environment.

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
import scala.concurrent.ExecutionContext.Implicits.global

val root = Root.fetch(None) // Future[Root]
val endpointHref = root.map(_.crestEndpoint.href) // Future[String]
// Lets print the contents of the Future.
endpointHref.foreach(println)
// Asynchonrously prints "https://crest-tq.eveonline.com/"
```

We can convert this to blocking code using the `Await` construct,
in the process removing the Future.
```scala
import scala.concurrent.Await
import scala.concurrent.duration._

val root = Root.fetch(None) // Future[Root]
// Block for at most 10 seconds to get the Root.
val rootResult = Await.result(root, 10 seconds) // Root
// Then print the href to the endpoint
println(rootResult.crestEndpoint.href)
```

For the following examples an `auth : Some[String]` variable is assumed,
which contains a string of the authentication token used to authenticate to the CREST API.

### Regions
We can get region information by following a link to Regions of the Root class.
Using Scala for-constructs, that looks as follows
```scala
val root = ... // Some Future[Root] instance
val region = for(
	rootRes <- root; // Extract Root
	region <- rootRes.regions.follow(auth) // Follow a CrestLink to Regions
) yield {
	// Asynchronously print the name of all regions
	println(region.items.map(_.name).mkString(", "))
	region
}
```

### Item Types
And another example of fetching item types, using `map` and `flatMap` instead.
```scala
val root = ... // Some Future[Root] instance
// Follow the link to the itemtype page.
val itemTypes = root.flatMap(_.itemTypes.follow(auth))
// Create a collection over all item type pages
val allItemTypes = itemTypes.flatMap(_.authedIterator(auth, retries=3).reduce)
allItemTypes.map(_.foreach { itemTypePage =>
	// Print each itemType name.
	itemTypePage.items.foreach(itemType => println(itemType.name))
})
```

## Implementation

## Features
- [ ] Modelling the entire CREST API.
- [ ] Creating tests for all models.
- [ ] Caching of API request.
- [ ] Improve interface.
	- Authentication token.
	- Hide disallowed methods.
	- Asynchronous iterator.
