import json
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from offerwave_slo_check import fetch, percentile


class _JsonHandler(BaseHTTPRequestHandler):
    business_code = 200

    def do_GET(self):
        payload = json.dumps({"code": self.business_code, "message": "test"}).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def log_message(self, _format, *_args):
        return


class OfferWaveSloCheckTest(unittest.TestCase):
    def setUp(self):
        self.server = ThreadingHTTPServer(("127.0.0.1", 0), _JsonHandler)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.url = f"http://127.0.0.1:{self.server.server_port}/jobs"

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=2)

    def test_fetch_accepts_http_and_business_success(self):
        _JsonHandler.business_code = 200

        sample = fetch(self.url, 1.0)

        self.assertTrue(sample.ok)
        self.assertIsNone(sample.error)

    def test_fetch_rejects_business_error_inside_http_200(self):
        _JsonHandler.business_code = 429

        sample = fetch(self.url, 1.0)

        self.assertFalse(sample.ok)
        self.assertEqual("business code 429", sample.error)

    def test_percentile_uses_nearest_rank(self):
        self.assertEqual(4.0, percentile([1.0, 2.0, 3.0, 4.0], 0.95))
        self.assertEqual(1.0, percentile([1.0], 0.95))


if __name__ == "__main__":
    unittest.main()
