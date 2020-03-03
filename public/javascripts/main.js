const from = new Date().toISOString().substring(0, 10) + "T00:00:00Z"
const limit = 10000

const reject = err => Promise.reject(new Error(err))

const displayStats = stats => {
  const videoGamesStats = document.getElementById("video-games");
  videoGamesStats.querySelectorAll(".badge")[0].innerHTML = stats.total;
  videoGamesStats.querySelectorAll(".badge")[1].innerHTML = stats.unrecognized.total;
  videoGamesStats.querySelectorAll(".badge")[2].innerHTML = stats.profitable.total;
}

fetch(`/api/video-games/summary?from=${from}`)
  .then(res => res.status == 200 ? res.json() : reject(`error getting video games status: ${res.status}`))
  .then(displayStats)
  .catch(err => console.error(err))