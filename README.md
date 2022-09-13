# kees/arena-graph

![I don't do much!](https://user-images.githubusercontent.com/6820950/189788029-f401bf54-31f8-47c5-9afd-abe829e07637.png)

---

## Upcoming:

- [x] [re-frame-http-fx](https://github.com/day8/re-frame-http-fx) to better integrate [are.na API](https://dev.are.na/documentation/channels) requests into re-frame handlers ([1](https://day8.github.io/re-frame/EffectfulHandlers/) [2](https://day8.github.io/re-frame/Effects/))
- [ ] Separate request flows
  - [ ] Original channel attributes / 0 order
  - [ ] Directly connected channels / 1st order
  - [ ] Mutually connected channels / 2nd order
- [ ] Node connectivity
  - [ ] All 1st order connect to 0 order
  - [ ] All 2nd order connect to relevant connections(!) not parents
- [ ] Node styling
  - [ ] Node size within variance by order
  - [ ] Node random color within variance by order
