#!/usr/bin/env bash
set -Eeuo pipefail
module_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
stage="$(mktemp -d)"
trap 'rm -rf -- "$stage"' EXIT
cp "$module_dir/frontier_expansion.go" "$module_dir/validation/expansion_test.go" "$stage/"
cp "$module_dir/validation/go.mod" "$stage/"
export FRONTIER_MODULE_DIR="$module_dir"
cd "$stage"
go mod tidy
gofmt -w *.go
go test -race -count=1 -v ./...
go vet ./...
