# kees/arena-graph

![I don't do much!](https://user-images.githubusercontent.com/6820950/189788029-f401bf54-31f8-47c5-9afd-abe829e07637.png)

## What:

The goal of this web app is visualize the [channels](https://support.are.na/help/whats-a-channel) an [are.na](https://www.are.na/) channel connects to, and their subsequent channel connections, in an interactive [undirected graph](https://en.wikipedia.org/wiki/Graph_(discrete_mathematics)#Graph).

The [d3 force graph layout](https://github.com/d3/d3-force) and [its react bindings](https://github.com/vasturiano/react-force-graph/) are really useful to make graphs that are kinetic, cute, fun, and descriptive, with very low effort using [reagent](https://github.com/reagent-project/reagent) in [CLJS](https://clojurescript.org/).

## And

Going beyond the second order of connections (single parent channel → children → grandchildren) seems too heavy for a public-facing web app using a medium-size social website. I think this is the appropriate scope.

Contributions are welcome. The UI framework I'm most comfortable with is [re-frame](https://github.com/day8/re-frame), so this whole app is built in clojurescript with re-frame.

Native language additions are easiest, however, the whole JS/React/Node package ecosystem is at arm's reach and running isolated JS scripts on demand (maybe TS too) is pretty easy too.

I haven't published a build yet. It's too rough and I want to stylize the UI to look coherent. You can test it with:

- A IDE supporting Clojure
- A web browser with CORS security policies disabled. <details>
  <summary>Example:</summary>

    ```sh
    rm -r /tmp/chro/ # Be careful
    open -na Chromium --args --disable-web-security --user-data-dir="/tmp/chro"
    ```

</details>

- A clone of this repo
- The commands below

## Upcoming:

<details open>
  <summary>Tasks</summary>

- [x] [re-frame-http-fx](https://github.com/day8/re-frame-http-fx) to better integrate [are.na API](https://dev.are.na/documentation/channels) requests into re-frame handlers ([1](https://day8.github.io/re-frame/EffectfulHandlers/) [2](https://day8.github.io/re-frame/Effects/))
- [x] Better status output
- [ ] Better division between state, logic, and flavor
- [ ] Separate request flows
  - [x] Original channel attributes / 0 order
  - [x] Directly connected channels / 1st order
  - [ ] Mutually connected channels / 2nd order
- [ ] Node connectivity
  - [x] All 1st order connect to 0 order
  - [ ] All 2nd order connect to relevant connections(!) not parents
- [x] Request pagination
- [x] Node styling
  - [x] Node size within variance by order
  - [x] Node random color within variance by order
- [ ] Oauth(?) realized it's unnecessary for an initial build
- [ ] UI refine
  - [ ] Kill nonessential inputs
  - [ ] Re-theme
  - [ ] Maximize content in space
  - [ ] Better loading

</details>

## What I'd welcome help with

- Optimizing the way the app makes series of potentially many API requests
- Further developing the styles of graph nodes and links. Basic react interop in CLJS is very easy but difficult to scale.
- More fun features to interact with the visualization
- Accessibility guidance

---

Run this to work on the project. For now you can include your own token in [rf.cljs](src/kees/arena_graph/rf.cljs). The token is only necessary for private channels.

```sh
npm i
npm run watch # Or jack in
open http://localhost:8280
```
