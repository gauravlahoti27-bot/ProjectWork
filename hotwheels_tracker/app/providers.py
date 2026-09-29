import os
import requests

class AvailabilityResult:
    def __init__(self, status, source="unknown", message=""):
        self.status = status
        self.source = source
        self.message = message

class DemoProvider:
    # State is stored in-memory only for local demonstration.
    state = {}

    def check(self, product):
        status = self.state.get(product["id"], "UNAVAILABLE")
        return AvailabilityResult(status, "demo", "Demo provider")

class AuthorizedHTTPProvider:
    '''
    Generic adapter for an API/feed you are explicitly authorized to call.

    Expected response examples:
      {"available": true}
      {"status": "AVAILABLE"}

    Set an endpoint through the provider-specific integration in your own
    deployment. Do not use this adapter against a service where automation
    is prohibited.
    '''
    def check(self, product):
        endpoint = os.getenv("AUTHORIZED_AVAILABILITY_API")
        if not endpoint:
            return AvailabilityResult("UNKNOWN", "authorized-http",
                                       "No authorized API endpoint configured.")

        try:
            r = requests.get(
                endpoint,
                params={"product_url": product["product_url"]},
                timeout=10
            )
            r.raise_for_status()
            data = r.json()
            if data.get("available") is True or str(data.get("status", "")).upper() == "AVAILABLE":
                return AvailabilityResult("AVAILABLE", "authorized-http")
            if data.get("available") is False or str(data.get("status", "")).upper() == "UNAVAILABLE":
                return AvailabilityResult("UNAVAILABLE", "authorized-http")
            return AvailabilityResult("UNKNOWN", "authorized-http", "Unrecognized response.")
        except Exception as exc:
            return AvailabilityResult("UNKNOWN", "authorized-http", str(exc))

def get_provider(name):
    if name == "demo":
        return DemoProvider()
    if name == "authorized-http":
        return AuthorizedHTTPProvider()
    return DemoProvider()
