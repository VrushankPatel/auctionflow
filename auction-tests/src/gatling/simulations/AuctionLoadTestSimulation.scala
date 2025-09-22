package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class AuctionLoadTestSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080") // Adjust to your API base URL
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .header("Authorization", "Bearer test-token") // Assuming a test token or disable auth for test

  // Feeder for bid amounts
  val amountFeeder = Iterator.continually(Map("amount" -> (100.0 + scala.util.Random.nextDouble() * 100.0)))

  // Scenario for general bidding load
  val biddingScenario = scenario("Bidding Load Test")
    .feed(amountFeeder)
    .exec(http("Place Bid")
      .post("/api/v1/auctions/1/bids") // Assume auction ID 1 exists
      .body(StringBody("""{"amount": ${amount}}""")).asJson
      .check(status.is(200))
    )

  // Scenario for anti-snipe: bids placed near the end
  val antiSnipeScenario = scenario("Anti-Snipe Load Test")
    .feed(amountFeeder)
    .exec(http("Place Bid Near End")
      .post("/api/v1/auctions/1/bids")
      .body(StringBody("""{"amount": ${amount}}""")).asJson
      .check(status.is(200))
    )

  setUp(
    biddingScenario.inject(
      rampUsers(8000).during(10.minutes) // Ramp up to 8000 users over 10 minutes
    ),
    antiSnipeScenario.inject(
      nothingFor(9.minutes), // Wait 9 minutes
      rampUsers(2000).during(1.minute) // Then 2000 users in last minute to trigger anti-snipe
    )
  ).protocols(httpProtocol)
}