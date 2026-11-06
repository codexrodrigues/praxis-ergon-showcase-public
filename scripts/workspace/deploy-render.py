"""Deploy the reviewed commit, then verify its terminal status and health."""
import json
import os
import subprocess
import time
import urllib.request

API = "https://api.render.com/v1"
EXPECTED_REPO = "https://github.com/codexrodrigues/praxis-ergon-showcase-public"


def main():
    token = os.environ["RENDER_API_KEY"]
    service_id = os.environ["RENDER_SERVICE_ID"]
    commit = os.environ["GITHUB_SHA"]
    if len(commit) != 40 or any(c not in "0123456789abcdef" for c in commit):
        raise RuntimeError("An exact source commit is required")

    def api(path, body=None):
        request = urllib.request.Request(API + path,
            data=None if body is None else json.dumps(body).encode(),
            headers={"Authorization": "Bearer " + token, "Content-Type": "application/json"})
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.load(response)

    service = api("/services/" + service_id)
    if service["repo"].removesuffix(".git") != EXPECTED_REPO or service["branch"] != "main":
        raise RuntimeError("Production service source differs from the reviewed target")
    if service.get("autoDeploy") != "no":
        raise RuntimeError("Disable Render auto-deploy before promoting the reviewed candidate")
    head = subprocess.check_output(["git", "ls-remote", EXPECTED_REPO + ".git", "refs/heads/main"], text=True).split()[0]
    if head != commit:
        raise RuntimeError("Candidate must be the exact public mirror main commit")
    deploy = api("/services/" + service_id + "/deploys", {"commitId": commit, "clearCache": "do_not_clear"})
    deploy_id = deploy["id"]
    deadline = time.monotonic() + 1100
    while time.monotonic() < deadline:
        deploy = api("/services/" + service_id + "/deploys/" + deploy_id)
        if deploy.get("commit", {}).get("id") != commit:
            raise RuntimeError("Render deployment source does not match the reviewed commit")
        status = deploy["status"]
        print("Deployment", deploy_id, status, flush=True)
        if status == "live":
            url = service["serviceDetails"]["url"] + "/actuator/health"
            with urllib.request.urlopen(url, timeout=30) as response:
                if response.status != 200 or json.load(response).get("status") != "UP":
                    raise RuntimeError("Candidate deployment health is not UP")
            return
        if status in {"build_failed", "update_failed", "pre_deploy_failed", "canceled", "deactivated"}:
            raise RuntimeError("Candidate deployment terminated: " + status)
        time.sleep(15)
    raise RuntimeError("Timed out waiting for the candidate deployment")


if __name__ == "__main__":
    main()
