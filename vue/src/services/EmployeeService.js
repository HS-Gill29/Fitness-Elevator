import axios from "axios";

export default {
  createEvent(event) {
    axios.post("/gym/schedule", event);
  },
  removeEvent(id) {
    axios.delete(`/gym/schedule/${id}`);
  },
  updateEvent(event) {
    axios.put(`/gym/schedule/${id}`, event);
  },
};
