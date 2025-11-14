import { sleep } from "k6"

export const randomInt = (min, max) => Math.floor(Math.random() * (max - min + 1)) + min
export const parseJsonReponse = (r, d = {}) => { try { return r.json() } catch(e) { return d } }
export const sleepWhen = (booleanValue, seconds) => sleep(booleanValue ? seconds : 0)