# kees/arena-graph

![I don't do much!](https://user-images.githubusercontent.com/6820950/189788029-f401bf54-31f8-47c5-9afd-abe829e07637.png)

---

## Upcoming:

- [x] [re-frame-http-fx](https://github.com/day8/re-frame-http-fx) to better integrate [are.na API](https://dev.are.na/documentation/channels) requests into re-frame handlers ([1](https://day8.github.io/re-frame/EffectfulHandlers/) [2](https://day8.github.io/re-frame/Effects/))
- [ ] Separate request flows
  - [x] Original channel attributes / 0 order
  - [x] Directly connected channels / 1st order
  - [ ] Mutually connected channels / 2nd order
- [ ] Node connectivity
  - [x] All 1st order connect to 0 order
  - [ ] All 2nd order connect to relevant connections(!) not parents
- [ ] Request pagination
- [x] Node styling
  - [x] Node size within variance by order
  - [x] Node random color within variance by order
- [ ] Oauth(?)
- [ ] UI refine
  - [ ] Kill nonessential inputs
  - [ ] Maximize content in space
  - [ ] Loading

---

Building this yourself? For now you need to include your own token in [rf.cljs](src/kees/arena_graph/rf.cljs).

```sh
npm i
npm run watch # Or jack in
```
