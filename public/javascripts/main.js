const from = new Date().toISOString().substring(0, 10) + "T00:00:00Z"
const limit = 10000

const reject = err => Promise.reject(new Error(err))

const displayStats = games => {
  const total = games.length
  const withoutResellPrice = games.filter(g => !g.resellPrice).length
  const withGoodPrice = games.filter(g => g.resellPrice && g.listingDetails.price < g.resellPrice.cash).length
  const videoGamesStats = document.getElementById("video-games")
  videoGamesStats.querySelector(".badge:nth-of-type(1)").innerHTML = total
  videoGamesStats.querySelector(".badge:nth-of-type(2)").innerHTML = withoutResellPrice
  videoGamesStats.querySelector(".badge:nth-of-type(3)").innerHTML = withGoodPrice
}

fetch(`/api/video-games?limit=${limit}&from=${from}`)
  .then(res => res.status == 200 ? res.json() : reject(`error getting video games: ${res.status}`))
  .then(displayStats)
  .catch(err => console.error(err))